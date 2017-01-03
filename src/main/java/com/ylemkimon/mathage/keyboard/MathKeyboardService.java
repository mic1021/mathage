package com.ylemkimon.mathage.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.ylemkimon.mathage.R;
import com.ylemkimon.mathage.settings.SettingsManager;
import com.ylemkimon.mathage.share.SaveToStorage;

import java.util.List;

public class MathKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private String mComposing = "";
    private MathKeyboardView mView;
    private Vibrator mVibrator;

    private String mPackageName = "";

    private boolean updated = false;

    @Override
    public void onCreate() {
        final SettingsManager sm = new SettingsManager(this);
        setTheme(sm.getKeyboardTheme());

        super.onCreate();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public View onCreateInputView() {
        mView = (MathKeyboardView) getLayoutInflater().inflate(R.layout.view_math_keyboard, null);
        mView.setKeyboardService(this);

        return mView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        mPackageName = info.packageName;

        mView.getKeyboard().setImeOptions(getResources(), info.imeOptions);
        updateClearIcon();
        mView.getKeyboardView().closing();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        mComposing = "";
        if (mView != null) {
            mView.appendMath(mComposing);
            mView.getKeyboardView().closing();
        }
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (updated) {
            updated = false;
            return;
        }

        boolean success = false;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && newSelStart == newSelEnd) {
            CharSequence before = ic.getTextBeforeCursor(100, 0);
            CharSequence after = ic.getTextAfterCursor(100, 0);
            if (before != null && after != null) {
                String beforeCursor = before.toString();
                String afterCursor = after.toString();

                int start = beforeCursor.lastIndexOf("\\[");
                int beforeEnd = beforeCursor.lastIndexOf("\\]");
                int end = afterCursor.indexOf("\\]");
                int afterStart = afterCursor.indexOf("\\[");
                if (start > -1 && end > -1 && beforeEnd < start && (afterStart == -1 || end < afterStart)) {
                    mVibrator.vibrate(10);

                    ic.setComposingRegion(newSelStart - beforeCursor.length() + start, newSelStart + end + 2);
                    mComposing = beforeCursor.substring(start + 2) + afterCursor.substring(0, end);
                    success = true;
                }
            }
        }
        if (!success) {
            if (ic != null)
                ic.finishComposingText();
            mComposing = "";
        }

        mView.appendMath(mComposing);
        updateClearIcon();
    }

    public void updateComposingText(String latex) {
        mVibrator.vibrate(10);
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            mComposing = latex;
            updated = true;
            ic.setComposingText("\\[" + mComposing + "\\]", 1);
        }
    }

    public void updateClearIcon() {
        mView.getKeyboard().setClearIcon(getResources(), mComposing.isEmpty() && mView.isEmpty());
        mView.getKeyboardView().invalidateKey(0);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        mVibrator.vibrate(10);

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            switch (primaryCode) {
                case -100:
                    if (mComposing.isEmpty()) {
                        if (mView.isEmpty()) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.showInputMethodPicker();
                        } else {
                            mView.appendMath("");
                            mComposing = "";
                            updateClearIcon();
                        }
                    } else
                        ic.commitText("", 1);
                    break;
                case -101:
                    mView.startMainActivity();
                    break;
                default:
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, primaryCode));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, primaryCode));
                    break;
            }
        }
    }

    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }

    public void shareImage() {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(shareIntent, 0);
        CharSequence name = "";
        if (!resInfo.isEmpty()) {
            for (ResolveInfo rinfo : resInfo) {
                if (rinfo.activityInfo.packageName.equals(mPackageName)) {
                    shareIntent.setPackage(rinfo.activityInfo.packageName);
                    name = rinfo.loadLabel(getPackageManager());
                    break;
                }
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (name.length() > 0) {
            CharSequence[] items = {name, getString(R.string.storage)};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 1)
                        shareIntent.setClass(MathKeyboardService.this, SaveToStorage.class);
                    mView.shareImage(shareIntent);
                }
            });
        } else {
            CharSequence[] items = {getString(R.string.storage)};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    shareIntent.setClass(MathKeyboardService.this, SaveToStorage.class);
                    mView.shareImage(shareIntent);
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}
