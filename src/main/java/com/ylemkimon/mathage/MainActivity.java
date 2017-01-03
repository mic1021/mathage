package com.ylemkimon.mathage;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ShareActionProvider;

import com.myscript.atk.maw.MathWidgetApi;
import com.ylemkimon.mathage.keyboard.MathKeyboardService;
import com.ylemkimon.mathage.settings.SettingsActivity;
import com.ylemkimon.mathage.settings.SettingsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity implements MathWidgetApi.OnConfigureListener, MathWidgetApi.OnUndoRedoListener, MathWidgetApi.OnRecognitionListener {
    public static final String EXTRA_MAW = "MAW";
    public static final String EXTRA_LATEX = "LATEX";

    private boolean mConfigured = false;

    private Menu mMenu;
    private ShareActionProvider mShareActionProviderLatex;
    private ShareActionProvider mShareActionProviderImage;

    private MathWidgetApi mWidget;
    private MathJaxView webView;

    private Vibrator mVibrator;

    private String mLatex = "";

    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = ProgressDialog.show(this, null, getString(R.string.please_wait));

        final SettingsManager sm = new SettingsManager(this);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        webView = (MathJaxView) findViewById(R.id.webView);
        webView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(10);
                appendMath();
                webView.updateMath(mLatex);
            }
        });

        mWidget = (MathWidgetApi) findViewById(R.id.myscript_maw);
        mWidget.setOnUndoRedoListener(this);
        mWidget.setOnConfigureListener(this);
        mWidget.setOnRecognitionListener(this);
        mWidget.setPaddingRatio(0.1f, 0, 0.6f, 0);
        mWidget.setBeautificationOption(MathWidgetApi.RecognitionBeautification.BeautifyFontify);
        mWidget.setPalmRejectionEnabled(true);
        mWidget.setPalmRejectionLeftHanded(sm.getPalm());

        String[] resources = new String[]{"math-ak.res", "math-grm-maw.res"};
        String resourcePath = getFilesDir().getPath() + "/math";
        try {
            File resourceFolder = new File(resourcePath);
            resourceFolder.mkdirs();
            for (String filename : resources) {
                File dst = new File(resourceFolder, filename);
                if (!dst.exists()) {
                    InputStream is = getAssets().open("math/" + filename);
                    byte[] buffer = new byte[is.available()];
                    is.read(buffer);
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
        mWidget.configure(this, resources, MyCertificate.getBytes(), MathWidgetApi.AdditionalGestures.DefaultGestures);

        boolean tutorial = true;
        String eula = "";
        if (!sm.getEula()) {
            BufferedReader in = null;
            try {
                StringBuilder buf = new StringBuilder();
                InputStream is = getAssets().open("eula.txt");
                in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str;
                while ((str = in.readLine()) != null) {
                    buf.append(str);
                    buf.append('\n');
                }
                eula = buf.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException ignored) {
                }
            }
        } else if (tutorial = !sm.getTutorial()) {
            boolean aEnabled = false;
            boolean imEnabled = false;

            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_VISUAL);
            for (AccessibilityServiceInfo service : runningServices) {
                if (service.getId().equals(new ComponentName(this, LatexService.class).flattenToShortString())) {
                    aEnabled = true;
                    break;
                }
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabledInputMethods = imm.getEnabledInputMethodList();
            for (InputMethodInfo method : enabledInputMethods) {
                if (method.getId().equals(new ComponentName(this, MathKeyboardService.class).flattenToShortString())) {
                    imEnabled = true;
                    break;
                }
            }
            tutorial = !aEnabled || !imEnabled;
        }

        if (tutorial)
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(!eula.isEmpty() ? R.string.alert_welcome_title : R.string.alert_enable_title)
                    .setMessage(!eula.isEmpty() ? R.string.alert_welcome_message : R.string.alert_enable_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(MainActivity.this, TutorialActivity.class));
                        }
                    })
                    .setNeutralButton(R.string.button_no_tutorial, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sm.setTutorial();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        if (!eula.isEmpty())
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.alert_eula_title)
                    .setMessage(eula)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sm.setEula();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_MAW)) {
            mWidget.unserialize(intent.getByteArrayExtra(EXTRA_MAW), true);
            mLatex = intent.getStringExtra(EXTRA_LATEX);
            webView.updateMath(mLatex);
            if (mMenu != null) {
                onUndoRedoStateChanged();
                onRecognitionEnd();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;

        mShareActionProviderLatex = (ShareActionProvider) menu.findItem(R.id.action_export_latex).getActionProvider();
        mShareActionProviderLatex.setShareIntent(new Intent(Intent.ACTION_SEND).setType("text/plain"));
        mShareActionProviderImage = (ShareActionProvider) menu.findItem(R.id.action_export_image).getActionProvider();
        mShareActionProviderImage.setShareIntent(new Intent(Intent.ACTION_SEND).setType("image/png"));

        if (mConfigured) {
            onUndoRedoStateChanged();
            onRecognitionEnd();
        } else {
            menu.findItem(R.id.action_undo).setEnabled(false);
            menu.findItem(R.id.action_redo).setEnabled(false);
            menu.findItem(R.id.action_clear).setEnabled(false);
            menu.findItem(R.id.action_export_image).setEnabled(false);
            menu.findItem(R.id.action_export_latex).setEnabled(false);
            menu.findItem(R.id.action_search).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mVibrator.vibrate(10);

        switch (item.getItemId()) {
            case R.id.action_undo:
                mWidget.undo();
                break;
            case R.id.action_redo:
                mWidget.redo();
                break;
            case R.id.action_clear:
                mWidget.clear(true);
                mLatex = "";
                webView.clearMath();
                break;
            case R.id.action_export_latex:
                appendMath();
                webView.updateMath(mLatex);
                mShareActionProviderLatex.setShareIntent(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "\\[" + mLatex + "\\]"));
                break;
            case R.id.action_export_image:
                appendMath();
                webView.updateAndCaptureMath(mLatex, mShareActionProviderImage);
                break;
            case R.id.action_search:
                appendMath();
                webView.getMathML(mLatex);
                break;
            case R.id.action_settings:
            case R.id.action_help:
                startActivity(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.EXTRA_HELP, item.getItemId()));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationBegin() {
    }

    @Override
    public void onConfigurationEnd(boolean success) {
        if (mConfigured = success)
            onNewIntent(getIntent());
        else
            webView.setMessage(mWidget.getErrorString());
        mProgressDialog.dismiss();
    }

    @Override
    public void onRecognitionBegin() {
    }

    @Override
    public void onRecognitionEnd() {
        boolean empty = mLatex.isEmpty() && mWidget.isEmpty();
        mMenu.findItem(R.id.action_clear).setEnabled(!empty);
        mMenu.findItem(R.id.action_export_image).setEnabled(!empty);
        mMenu.findItem(R.id.action_export_latex).setEnabled(!empty);
        mMenu.findItem(R.id.action_search).setEnabled(!empty);
    }

    @Override
    public void onUndoRedoStateChanged() {
        mMenu.findItem(R.id.action_undo).setEnabled(mWidget.canUndo());
        mMenu.findItem(R.id.action_redo).setEnabled(mWidget.canRedo());
    }

    private void appendMath() {
        mLatex += mWidget.getResultAsLaTeX();
        mWidget.clear(true);
    }
}