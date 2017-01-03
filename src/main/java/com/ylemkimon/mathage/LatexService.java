package com.ylemkimon.mathage;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.ylemkimon.mathage.keyboard.MathKeyboardService;
import com.ylemkimon.mathage.settings.SettingsManager;

import java.util.ArrayList;
import java.util.Set;

public class LatexService extends AccessibilityService {
    private String mHotkey;
    private ArrayList<AccessibilityNodeInfo> mNodeList = new ArrayList<>();
    private ArrayList<ImageView> mButtonList = new ArrayList<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

            getLaTeXFromSource(event.getSource());
            for (ImageView button : mButtonList) {
                try {
                    wm.removeView(button);
                } catch (Exception ignored) {
                }
            }
            mButtonList.clear();
            for (AccessibilityNodeInfo info : mNodeList) {
                Rect outbound = new Rect();
                info.getBoundsInScreen(outbound);

                ImageView button = new ImageView(this);
                button.setImageResource(R.mipmap.ic_launcher);
                button.setContentDescription(info.getText().toString());

                final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        80,
                        80,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);

                params.gravity = Gravity.TOP | Gravity.LEFT;
                params.x = outbound.right - 80;
                params.y = outbound.bottom - 80;

                wm.addView(button, params);
                mButtonList.add(button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLaTeXDialog(v.getContentDescription().toString());
                    }
                });
            }
            mNodeList.clear();
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED && !event.getClassName().equals(EditText.class.getName())) {
            String str;
            if (!event.getText().isEmpty())
                str = event.getText().get(0).toString();
            else if (event.getContentDescription() != null)
                str = event.getContentDescription().toString();
            else
                return;
            if (hasLatex(str))
                showLaTeXDialog(str);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED && event.getAddedCount() == 1 && !event.isPassword() && !event.getText().isEmpty()) {
            String beforeText = event.getBeforeText() != null ? event.getBeforeText().toString() : "";
            if (event.getText().get(0).length() - event.getText().get(0).toString().replace(mHotkey, "").length()
                    - beforeText.length() + beforeText.replace(mHotkey, "").length() > 0
                    && !Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD)
                    .equals(new ComponentName(this, MathKeyboardService.class).flattenToShortString())) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            }
        }
    }

    private void getLaTeXFromSource(AccessibilityNodeInfo source) {
        if (source != null) {
            if (source.getClassName() != null && !source.getClassName().equals(EditText.class.getName()) && source.getText() != null && hasLatex(source.getText().toString()) && !source.isClickable())
                mNodeList.add(source);
            for (int i = 0; i < source.getChildCount(); i++) {
                getLaTeXFromSource(source.getChild(i));
            }
        }
    }

    private void showLaTeXDialog(final String str) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(10);
        startActivity(new Intent(this, LatexPopupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(LatexPopupActivity.EXTRA_LATEX, str));
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        SettingsManager sm = new SettingsManager(this);
        Set<String> apps = sm.getAppList();
        info.packageNames = apps.toArray(new String[apps.size()]);
        mHotkey = sm.getHotkey();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_VIEW_SCROLLED | AccessibilityEvent.TYPE_VIEW_CLICKED | (mHotkey.length() > 0 ? AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED : 0);
        setServiceInfo(info);
    }

    private static boolean hasLatex(String str) {
        return hasLatex(str, "\\(", "\\)") || hasLatex(str, "\\[", "\\]") || hasLatex(str, "$$", "$$");
    }

    private static boolean hasLatex(String s, String s1, String s2) {
        return s.contains(s1) && s.indexOf(s1) < s.lastIndexOf(s2);
    }
}