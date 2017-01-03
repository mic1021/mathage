package com.ylemkimon.mathage.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;

import com.ylemkimon.mathage.R;
import com.ylemkimon.mathage.settings.SettingsManager;

import java.util.Arrays;

public class MathKeyboard extends Keyboard {
    private Key mEnterKey;
    private Key mClearKey;

    private static int[] drawables = new int[10];

    public MathKeyboard(Context context) {
        super(init(context), R.xml.keyboard);
    }

    private static Context init(Context context) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.themeName, tv, true);
        if (tv.string.equals(SettingsManager.THEME_LXX))
            drawables = new int[]{R.drawable.sym_keyboard_delete_lxx_light, //0
                    R.drawable.sym_keyboard_done_lxx_light, //1
                    R.drawable.sym_keyboard_go_lxx_light, //2
                    R.drawable.sym_keyboard_next_lxx_light, //3
                    R.drawable.sym_keyboard_return_lxx_light, //4
                    R.drawable.sym_keyboard_search_lxx_light, //5
                    R.drawable.sym_keyboard_send_lxx_light, //6
                    R.drawable.sym_keyboard_settings_lxx_light, //7
                    R.drawable.sym_keyboard_smiley_lxx_light, //8
                    R.drawable.sym_keyboard_space_lxx_light}; //9
        else if (tv.string.equals(SettingsManager.THEME_LXX_DARK))
            drawables = new int[]{R.drawable.sym_keyboard_delete_lxx_dark, //0
                    R.drawable.sym_keyboard_done_lxx_dark, //1
                    R.drawable.sym_keyboard_go_lxx_dark, //2
                    R.drawable.sym_keyboard_next_lxx_dark, //3
                    R.drawable.sym_keyboard_return_lxx_dark, //4
                    R.drawable.sym_keyboard_search_lxx_dark, //5
                    R.drawable.sym_keyboard_send_lxx_dark, //6
                    R.drawable.sym_keyboard_settings_lxx_dark, //7
                    R.drawable.sym_keyboard_smiley_lxx_dark, //8
                    R.drawable.sym_keyboard_space_lxx_dark}; //9
        else //if (tv.string.equals("holo"))
            drawables = new int[]{R.drawable.sym_keyboard_delete_holo_dark, //0
                    R.drawable.sym_keyboard_return_holo_dark, //1
                    R.drawable.sym_keyboard_return_holo_dark, //2
                    R.drawable.sym_keyboard_return_holo_dark, //3
                    R.drawable.sym_keyboard_return_holo_dark, //4
                    R.drawable.sym_keyboard_search_holo_dark, //5
                    R.drawable.sym_keyboard_return_holo_dark, //6
                    R.drawable.sym_keyboard_settings_holo_dark, //7
                    R.drawable.sym_keyboard_smiley_holo_dark, //8
                    R.drawable.sym_keyboard_space_holo_dark}; //9
        return context;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        Key key = new MathKey(res, parent, x, y, parser);

        switch (key.codes[0]) {
            case -100:
                mClearKey = key;
                key.icon = res.getDrawable(drawables[7]);
                break;
            case -101:
                key.icon = res.getDrawable(drawables[8]);
                break;
            case 62:
                key.icon = res.getDrawable(drawables[9]);
                break;
            case 67:
                key.icon = res.getDrawable(drawables[0]);
                break;
            case 66:
                mEnterKey = key;
                key.icon = res.getDrawable(drawables[4]);
                break;
        }
        return key;
    }

    @SuppressWarnings("deprecation")
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null)
            return;
        switch (options & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.icon = res.getDrawable(drawables[2]);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.icon = res.getDrawable(drawables[3]);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = res.getDrawable(drawables[5]);
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.icon = res.getDrawable(drawables[6]);
                break;
            case EditorInfo.IME_ACTION_DONE:
                mEnterKey.icon = res.getDrawable(drawables[1]);
                break;
            default:
                mEnterKey.icon = res.getDrawable(drawables[4]);
                break;
        }
    }

    @SuppressWarnings("deprecation")
    void setClearIcon(Resources res, boolean empty) {
        if (mClearKey != null)
            mClearKey.icon = res.getDrawable(drawables[empty ? 7 : 0]);
    }

    public static class MathKey extends Key {
        public MathKey(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }

        @Override
        public int[] getCurrentDrawableState() {
            if (codes[0] == 66) {
                int[] states = super.getCurrentDrawableState();
                states = Arrays.copyOf(states, states.length + 1);
                states[states.length - 1] = android.R.attr.state_active;
                return states;
            }
            return super.getCurrentDrawableState();
        }
    }
}
