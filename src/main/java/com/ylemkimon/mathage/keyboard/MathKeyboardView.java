package com.ylemkimon.mathage.keyboard;

import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.myscript.atk.maw.MathWidgetApi;
import com.ylemkimon.mathage.MainActivity;
import com.ylemkimon.mathage.MathJaxView;
import com.ylemkimon.mathage.MyCertificate;
import com.ylemkimon.mathage.R;
import com.ylemkimon.mathage.settings.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MathKeyboardView extends RelativeLayout implements MathWidgetApi.OnConfigureListener, MathWidgetApi.OnRecognitionListener {
    private Context mContext;

    private MathWidgetApi mWidget;
    private MathJaxView webView;

    private String mLatex = "";

    private MathKeyboard keyboard;
    private MathInsideKeyboardView keyboardView;

    private MathKeyboardService mathKeyboardService;

    public MathKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        webView = (MathJaxView) findViewById(R.id.webView);
        webView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                appendMath();
                if (mathKeyboardService != null)
                    mathKeyboardService.updateComposingText(mLatex);
                webView.updateMath(mLatex);
            }
        });

        keyboardView = (MathInsideKeyboardView) findViewById(R.id.keyboard);
        keyboard = new MathKeyboard(mContext);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setPreviewEnabled(false);


        final SettingsManager sm = new SettingsManager(mContext);
        mWidget = (MathWidgetApi) findViewById(R.id.myscript_maw);
        mWidget.setOnRecognitionListener(this);
        mWidget.setOnConfigureListener(this);
        mWidget.setPaddingRatio(0.1f, 0, 0.6f, 0);
        mWidget.setBeautificationOption(MathWidgetApi.RecognitionBeautification.BeautifyFontify);
        mWidget.setPalmRejectionEnabled(true);
        mWidget.setPalmRejectionLeftHanded(sm.getPalm());

        String[] resources = new String[]{"math-ak.res", "math-grm-maw.res"};
        String resourcePath = mContext.getFilesDir().getPath() + "/math";
        try {
            File resourceFolder = new File(resourcePath);
            resourceFolder.mkdirs();

            for (String filename : resources) {
                InputStream is = mContext.getAssets().open("math/" + filename);
                byte[] buffer = new byte[is.available()];
                is.read(buffer);

                File dst = new File(resourceFolder, filename);
                if (!dst.exists()) {
                    dst.createNewFile();
                    FileOutputStream out = new FileOutputStream(dst);
                    out.write(buffer);
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mWidget.setResourcesPath(resourcePath);

        mWidget.configure(mContext, resources, MyCertificate.getBytes(), MathWidgetApi.AdditionalGestures.DefaultGestures);
    }

    private void appendMath() {
        mLatex += mWidget.getResultAsLaTeX();
        mWidget.clear(true);
    }

    public void appendMath(String latex) {
        mLatex = latex;
        if (!mWidget.isEmpty())
            mWidget.clear(false);
        if (!mLatex.isEmpty())
            webView.updateMath(mLatex);
        else
            webView.clearMath();
    }

    public void setKeyboardService(MathKeyboardService mathKeyboardService) {
        this.mathKeyboardService = mathKeyboardService;
        keyboardView.setKeyboardService(mathKeyboardService);
        keyboardView.setOnKeyboardActionListener(mathKeyboardService);
    }

    public MathKeyboard getKeyboard() {
        return keyboard;
    }

    public KeyboardView getKeyboardView() {
        return keyboardView;
    }

    public boolean isEmpty() {
        return mWidget.isEmpty();
    }

    public void startMainActivity() {
        Intent i = new Intent(mathKeyboardService, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_MAW, mWidget.serialize())
                .putExtra(MainActivity.EXTRA_LATEX, mLatex);
        mathKeyboardService.startActivity(i);
    }

    public void shareImage(Intent shareIntent) {
        appendMath();
        if (!mLatex.isEmpty())
            webView.updateAndCaptureMath(mLatex, shareIntent);
    }

    @Override
    public void onConfigurationBegin() {
    }

    @Override
    public void onConfigurationEnd(boolean success) {
        if (!success)
            webView.setMessage(mWidget.getErrorString());
    }

    @Override
    public void onRecognitionBegin() {
    }

    @Override
    public void onRecognitionEnd() {
        mathKeyboardService.updateClearIcon();
    }

    public static class MathInsideKeyboardView extends KeyboardView {
        private MathKeyboardService mathKeyboardService;

        public MathInsideKeyboardView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setKeyboardService(MathKeyboardService mathKeyboardService) {
            this.mathKeyboardService = mathKeyboardService;
        }

        @Override
        protected boolean onLongPress(Keyboard.Key popupKey) {
            if (popupKey.codes[0] == 66)
                mathKeyboardService.shareImage();
            return super.onLongPress(popupKey);
        }
    }
}
