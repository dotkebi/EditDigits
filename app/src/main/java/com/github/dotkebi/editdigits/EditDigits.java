package com.github.dotkebi.editdigits;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Edit Digits
 * @author by dotkebi@gmail.com on 2015-09-23.
 */
public class EditDigits extends EditText {
    private static final int SET_COMMA = 7462;
    private static final int BRING_CURSOR_TO_LAST_POSITION = 7461;
    private static final int REMOVE_FIRST_CHAR_AT_CURSOR_POSITION = 7460;
    private static final int HIDE_KEYBOARD = 7459;

    private static final long KEY_INTERVAL = 50;

    private static final char period = '.';
    private static final char dash = '-';

    private EditDigitsHandler handler;

    private Context context;

    private int previousCursorPosition;
    private int quantityOfPeriodBeforeCursor;

    private boolean blockSoftKey;
    //private boolean blockHardKey;
    private boolean hasFocus;
    private boolean autoHideKeyboard;

    public EditDigits(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        blockSoftKey = false;
        //blockHardKey = false;
        hasFocus = false;

        handler = new EditDigitsHandler(this);

        setGravity(Gravity.RIGHT);
        setFilters(new InputFilter[]{filterNumberMinus});
        addTextChangedListener(new DigitsWatcher());
        setKeyListener(DigitsKeyListener.getInstance("0123456789,.-"));

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditDigits);
        if (a != null) {
            autoHideKeyboard = a.getBoolean(R.styleable.EditDigits_autoHideKeyboard, false);
            a.recycle();
        }

        if (autoHideKeyboard) {
            int type = this.getInputType();
            this.setInputType(InputType.TYPE_NULL);
            this.setRawInputType(type);
            this.setTextIsSelectable(true);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        final int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN && !hasFocus) {
            /*hasFocus = true;
            handler.sendEmptyMessageDelayed(BRING_CURSOR_TO_LAST_POSITION, 100);*/
            initRequestFocus();
        }
        if (autoHideKeyboard) {
            handler.sendEmptyMessage(HIDE_KEYBOARD);
        }
        return super.onTouchEvent(event);
    }

    public boolean initRequestFocus() {
        hasFocus = true;
        handler.sendEmptyMessageDelayed(BRING_CURSOR_TO_LAST_POSITION, 100);
        return super.requestFocus();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (!focused && hasFocus) {
            hasFocus = false;
            doAfterChanged(getText());
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            removeFirstCharAtCursorPosition();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setValue(float value) {
        doAfterChanged(String.valueOf(value));
    }

    public void setValue(int value) {
        doAfterChanged(String.valueOf(value));
    }

    public double getValue() {
        String str = getText().toString().replaceAll(",", "");

        String front;
        String end;
        int firstIndex = str.indexOf(period);
        if (firstIndex == -1) {
            front = str;
            end = "";
        } else {
            front = str.substring(0, firstIndex);
            end = str.substring(firstIndex + 1, str.length());
        }

        int size = end.length();
        String value = front + end;
        return Double.valueOf(value) / Math.pow(10, size);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    private void removeFirstCharAtCursorPosition() {
        String text = getText().toString();
        recordCursorPosition(text);

        if (text.length() == 0) {
            return;
        }

        if (text.length() == 1) {
            clearText();
            return;
        }

        int startPosition = getSelectionStart() - 1;
        int endPosition = getSelectionStart();

        if (startPosition < 0) {
            return;
        }

        --previousCursorPosition;
        if (text.charAt(startPosition) == ','
                || text.charAt(startPosition) == '.') {
            --startPosition;
            --previousCursorPosition;
        }

        String front = text.substring(0, startPosition);
        String end = text.substring(endPosition, text.length());

        sendSetText(front + end);
    }

    private void sendSetText(String value) {
        Message message = new Message();
        message.what = SET_COMMA;
        message.obj = value;
        handler.sendMessage(message);
    }

    private void bringCursorToLastPosition() {
        setCursorVisible(false);
        setSelection(getText().length());
        setCursorVisible(true);
        blockSoftKey = false;
    }

    private void doSetText(String value) {
        blockSoftKey = true;
        boolean minus = false;
        setCursorVisible(false);
        try {
            if (TextUtils.isEmpty(value)) {
                return;
            }
            value = value.replaceAll(",", "");
            if (value.charAt(0) == dash) {
                minus = true;
                value = value.substring(1, value.length());
            }

            final int dotPos = value.indexOf('.');
            String value1, value2;
            if (dotPos >= 0) {
                value1 = value.substring(0, dotPos);
                value2 = value.substring(dotPos, value.length());
            } else {
                value1 = value;
                value2 = "";
            }

            final String retTxt = value1;
            final int index = retTxt.length() - 1;

            StringBuilder sb = new StringBuilder();
            int dotIndex = 0;

            for (int i = index; i >= 0; i--, dotIndex++) {
                char ch = retTxt.charAt(i);
                if (dotIndex % 3 == 0 && i < index) {
                    sb.append(',');
                    ++previousCursorPosition;
                }
                sb.append(ch);
            }
            clearText();
            String msg = ((minus) ? dash : "") + sb.reverse().toString() + value2;
            setText(msg);

            previousCursorPosition = previousCursorPosition - quantityOfPeriodBeforeCursor;
            if (previousCursorPosition < 0) {
                previousCursorPosition = 0;
            }
            quantityOfPeriodBeforeCursor = 0;
            setSelection(previousCursorPosition);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            clearText();
        }
        setCursorVisible(true);
        blockSoftKey = false;
    }

    public void clearText() {
        if (getText().length() > 0) {
            setSelection(0);
            getText().clear();
        }
    }

    private void doAfterChanged(Editable s) {
        doAfterChanged(s.toString());
    }

    private void doAfterChanged(String source) {
        if (blockSoftKey) {
            return;
        }

        recordCursorPosition(source);
        String str = source.replaceAll(",", "");
        if (TextUtils.isEmpty(str)) {
            clearText();
            return;
        }

        int firstIndex = str.indexOf(period);
        if (firstIndex == 0) {
            sendSetText("0.");
            return;
        }

        String front;
        String end;

        if (firstIndex > -1) {
            int lastIndex = str.lastIndexOf(period);
            if (str.indexOf(period) != lastIndex) {
                front = str.substring(0, lastIndex);
                end = str.substring(lastIndex + 1, str.length());
                str = front + end;
                --previousCursorPosition;
            }
            front = str.substring(0, firstIndex);
            end = str.substring(firstIndex, str.length());

            if (front.length() > 10) {
                String front1 = str.substring(0, 10);
                String front2 = str.substring(10, str.length());

                headTrim(front1, front2 + end);
                return;
            } else {
                headTrim(front, end);
                return;
            }
        }
        if (str.length() > 10) {
            front = str.substring(0, 10);
            end = str.substring(10, str.length());
        } else {
            front = str;
            end = "";
        }
        headTrim(front, end);
    }

    private void recordCursorPosition(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                quantityOfPeriodBeforeCursor++;
            }
        }
        previousCursorPosition = getSelectionStart();
    }

    private void headTrim(String front, String end) {
        boolean minus = false;
        if (front.length() > 0 && front.charAt(0) == dash) {
            minus = true;
            front = front.replaceAll(String.valueOf(dash), "");
        }

        if (TextUtils.isEmpty(front)) {
            sendSetText(String.valueOf(dash));
            return;
        }

        Double value = Double.valueOf(front);
        BigDecimal bigDecimal = new BigDecimal(value);
        String retTxt = bigDecimal.toPlainString() + end;
        sendSetText(((minus) ? "-" : "") + String.valueOf(retTxt));
    }

    private class DigitsWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (blockSoftKey) {
                return;
            }

            String str = s.toString();
            int start = str.indexOf(dash);
            int end = str.lastIndexOf(dash);

            if (s.length() > 0 && start > 0 && end == start
                    || start != end) {
                //textFieldInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                blockSoftKey = true;
                s.delete(end, s.length());
                blockSoftKey = false;
            }
            doAfterChanged(s);
        }
    }

    //BaseInputConnection textFieldInputConnection = new BaseInputConnection(this, true);


    private Pattern pattern = Pattern.compile("^[0-9,.-]*$");
    private InputFilter filterNumberMinus = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (!pattern.matcher(source).matches()) {
                return "";
            }
            return null;
        }
    };


    private static class EditDigitsHandler extends Handler {
        private final WeakReference<EditDigits> weakBody;

        public EditDigitsHandler(EditDigits klass) {
            weakBody = new WeakReference<EditDigits>(klass);
        }

        @Override
        public void handleMessage(Message msg) {
            EditDigits klass = weakBody.get();

            switch (msg.what) {
                case BRING_CURSOR_TO_LAST_POSITION:
                    klass.bringCursorToLastPosition();
                    break;

                case SET_COMMA:
                    String value = (String) msg.obj;
                    klass.doSetText(value);
                    //sendEmptyMessage(BRING_CURSOR_TO_LAST_POSITION);
                    break;

                case REMOVE_FIRST_CHAR_AT_CURSOR_POSITION:
                    klass.removeFirstCharAtCursorPosition();
                    sendEmptyMessageDelayed(REMOVE_FIRST_CHAR_AT_CURSOR_POSITION, KEY_INTERVAL);
                    break;

                case HIDE_KEYBOARD:
                    klass.hideKeyboard();
                    break;
            }
        }
    }
}
