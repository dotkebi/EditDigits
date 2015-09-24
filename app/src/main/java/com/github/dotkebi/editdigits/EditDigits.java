package com.github.dotkebi.editdigits;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;

/**
 * Edit Digits
 * @author by dotkebi@gmail.com on 2015-09-23.
 */
public class EditDigits extends EditText {
    private static final int SET_COMMA = 7462;
    private static final int BRING_CURSOR_TO_LAST_POSITION = 7461;
    private static final int REMOVE_FIRST_CHAR_AT_CURSOR_POSITION = 7460;

    private static final long KEY_INTERVAL = 50;

    private static final String period = "\\.";

    private EditDigitsHandler handler;

    private int previousCursorPosition;
    private int quantityOfPeriodBeforeCursor;

    private boolean blockSoftKey;
    private boolean blockHardKey;
    private boolean hasFocus;

    public EditDigits(Context context) {
        this(context, null);
        init();
    }

    public EditDigits(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditDigits(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        blockSoftKey = false;
        blockHardKey = false;
        hasFocus = false;
        handler = new EditDigitsHandler(this);

        //setInputType(InputType.TYPE_CLASS_NUMBER);
        //setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        setGravity(Gravity.RIGHT);
        addTextChangedListener(new DigitsWatcher());

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP && !hasFocus) {
            hasFocus = true;
            handler.sendEmptyMessage(BRING_CURSOR_TO_LAST_POSITION);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (!focused && hasFocus) {
            hasFocus = false;
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            //blockHardKey = true;
            //Log.w("pressed", "down");
            removeFirstCharAtCursorPosition();
            //handler.sendEmptyMessage(REMOVE_FIRST_CHAR_AT_CURSOR_POSITION);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

/*    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (blockHardKey) {// && event.isTracking()) {
                 Log.w("pressed", "up");
                //handler.removeMessages(REMOVE_FIRST_CHAR_AT_CURSOR_POSITION);
                removeFirstCharAtCursorPosition();
                blockHardKey = false;
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }*/

    /*@Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {

                blockHardKey = true;
                handler.sendEmptyMessageDelayed(REMOVE_FIRST_CHAR_AT_CURSOR_POSITION, KEY_INTERVAL);
                return true;

            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (blockHardKey) {
                    handler.removeMessages(REMOVE_FIRST_CHAR_AT_CURSOR_POSITION);
                    //removeFirstCharAtCursorPosition();
                    blockHardKey = false;
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }*/

    private void removeFirstCharAtCursorPosition() {
        String text = getText().toString();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',') {
                quantityOfPeriodBeforeCursor++;
            }
        }
        previousCursorPosition = getSelectionStart();


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
        if (text.charAt(startPosition) == ',') {
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
        //setCursorVisible(false);
        try {
            if (TextUtils.isEmpty(value)) {
                return;
            }
            value = value.replaceAll(",", "");

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
            setText(sb.reverse().toString() + value2);

            //Log.w("size", getText() + " / " + getText().length() + " / " + previousCursorPosition + " - " + quantityOfPeriodBeforeCursor);

            previousCursorPosition = previousCursorPosition - quantityOfPeriodBeforeCursor;
            if (previousCursorPosition < 0) {
                previousCursorPosition = 0;
            }
            quantityOfPeriodBeforeCursor = 0;
            setSelection(previousCursorPosition);
            setCursorVisible(true);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            clearText();
            setText("0");
        }
        blockSoftKey = false;
    }

    private void clearText() {
        if (getText().length() > 0) {
            setSelection(0);
            getText().clear();
        }
    }

    private void doAfterChanged(Editable s) {
        if (blockSoftKey) {
            return;
        }
        Log.w("text", s.toString());

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                quantityOfPeriodBeforeCursor++;
            }
        }
        previousCursorPosition = getSelectionStart();
        String str = s.toString().replace(",", "");
        if (TextUtils.isEmpty(str)) {
            clearText();
            return;
        }

        int firstIndex = str.indexOf(period);
//        Log.w("period", "" + firstIndex);
        if (firstIndex == 0) {
            setText("0.");
            return;
        }

        String front;
        String end;
        int length = str.length();

        if (firstIndex > -1) {
            int lastIndex = str.lastIndexOf(period);
            if (str.indexOf(period) != lastIndex) {
                front = str.substring(0, lastIndex - 1);
                end = str.substring(lastIndex, str.length());
                str = front + end;
                Log.w("period", str);
            }
        }

        if (str.length() > 10) {
            if (firstIndex > 10) {
                firstIndex = 10;
            }
            front = str.substring(0, firstIndex);
            end = str.substring(firstIndex, str.length());
        } else {
            front = str;
            end = "";
        }

        Double value = Double.valueOf(front);
        BigDecimal bigDecimal = new BigDecimal(value);
        String retTxt = bigDecimal.toPlainString() + end;
        sendSetText(String.valueOf(retTxt));
    }



    private class DigitsWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            doAfterChanged(s);
        }
    }


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
            }
        }
    }
}
