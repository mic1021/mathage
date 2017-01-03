package com.ylemkimon.mathage;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ShareActionProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MathJaxView extends WebView {
    private boolean typesetDone = false;
    private boolean onDrawListener = false;
    private ShareActionProvider sap;

    private Context mContext;
    private ProgressDialog progressDialog;

    private OnClickListener onClickListener;

    private String mEditTextHint = "";
    private Intent shareIntent;

    public MathJaxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MathJaxView, 0, 0);
        mEditTextHint = a.getString(R.styleable.MathJaxView_hintText);
        if (!isInEditMode())
            init();
    }

    private void init() {
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(this, "Android");
        loadDataWithBaseURL("http://bar/",
                "<style>body{background-color: dimgray;}span{color: #FFF !important;}</style><script type='text/x-mathjax-config'>MathJax.Hub.Config({showMathMenu: false, messageStyle: 'none', jax: ['input/TeX','output/HTML-CSS'], extensions: ['tex2jax.js','toMathML.js'], TeX:{extensions: ['AMSmath.js','AMSsymbols.js','noErrors.js','noUndefined.js']}});</script><script type='text/javascript' src='http://cdn.mathjax.org/mathjax/latest/MathJax.js'></script><script type='text/javascript'>function toMathML(jax,callback){var mml;try{mml=jax.root.toMathML('');}catch(err){if(!err.restart)throw err;return MathJax.Callback.After([toMathML,jax,callback],err.restart);}MathJax.Callback(callback)(mml);}</script><span id='math'>"
                        + mEditTextHint
                        + "</span>", null, "utf-8", null);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
        setOnTouchListener(new OnTouchListener() {
            private final float MOVE_THRESHOLD_DP = 20 * getResources().getDisplayMetrics().density;
            private boolean mMoveOccured;
            private float mDownPosX;
            private float mDownPosY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mMoveOccured = false;
                        mDownPosX = event.getX();
                        mDownPosY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!mMoveOccured && onClickListener != null)
                            onClickListener.onClick(MathJaxView.this);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getX() - mDownPosX) > MOVE_THRESHOLD_DP || Math.abs(event.getY() - mDownPosY) > MOVE_THRESHOLD_DP)
                            mMoveOccured = true;
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (onDrawListener && typesetDone) {
            onDrawListener = false;
            typesetDone = false;
            buildDrawingCache();
            Bitmap result = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(result);
            Paint paint = new Paint();
            int iHeight = result.getHeight();
            c.drawBitmap(result, 0, iHeight, paint);
            draw(c);
            result = removeMargins(result);

            File file = new File(mContext.getCacheDir(), "temp.png");
            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(file);
                result.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                file.setReadable(true, false);

                if (sap != null)
                    sap.setShareIntent(new Intent(Intent.ACTION_SEND)
                            .setType("image/png")
                            .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file)));
                else
                    mContext.startActivity(shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (progressDialog != null)
                progressDialog.dismiss();
        }
    }

    public void setMessage(String msg) {
        loadDataWithBaseURL(null, msg, null, "utf-8", null);
        setOnClickListener(null);
    }

    private void setMath(String str, boolean latex) {
        if (latex)
            str = "\\\\[" + doubleEscapeTeX(str) + "\\\\]";
        evaluateJavascript("document.getElementById('math').innerHTML='" + str + "';", null);
    }

    private void typesetMath(String queue) {
        if (queue == null)
            evaluateJavascript("MathJax.Hub.Queue(['Typeset',MathJax.Hub]);", null);
        else
            evaluateJavascript("MathJax.Hub.Queue(['Typeset',MathJax.Hub],function(){" + queue + "});", null);
    }

    public void updateMath(String latex) {
        if (!latex.isEmpty()) {
            setMath(latex, true);
            typesetMath(null);
        } else
            clearMath();
    }

    public void getMathML(String latex) {
        setMath(latex, true);
        typesetMath("toMathML(MathJax.Hub.getAllJax()[0],function(mml){Android.searchMathML(mml)})");
    }

    public void clearMath() {
        setMath(mEditTextHint, false);
    }

    public void updateAndCaptureMath(String latex, ShareActionProvider sap) {
        this.sap = sap;

        progressDialog = new ProgressDialog(mContext);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(mContext.getString(R.string.please_wait));
        progressDialog.show();

        updateAndCaptureMath(latex);
    }

    public void updateAndCaptureMath(String latex, Intent shareIntent) {
        this.shareIntent = shareIntent;
        updateAndCaptureMath(latex);
    }

    private void updateAndCaptureMath(String latex) {
        setMath(latex, true);
        onDrawListener = true;
        typesetMath("Android.typesetDone()");
    }

    private static Bitmap removeMargins(Bitmap bmp) {
        int MTop = 0, MBot = 0, MLeft = 0, MRight = 0;
        boolean found = false;

        int[] bmpIn = new int[bmp.getWidth() * bmp.getHeight()];
        int[][] bmpInt = new int[bmp.getWidth()][bmp.getHeight()];

        bmp.getPixels(bmpIn, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

        for (int ii = 0, contX = 0, contY = 0; ii < bmpIn.length; ii++) {
            bmpInt[contX][contY] = bmpIn[ii];
            contX++;
            if (contX >= bmp.getWidth()) {
                contX = 0;
                contY++;
                if (contY >= bmp.getHeight()) {
                    break;
                }
            }
        }

        for (int hP = 0; hP < bmpInt[0].length && !found; hP++) {
            for (int wP = 0; wP < bmpInt.length; wP++) {
                if (bmpInt[wP][hP] != Color.rgb(105, 105, 105)) {
                    MTop = hP;
                    found = true;
                    break;
                }
            }
        }
        found = false;

        for (int hP = bmpInt[0].length - 1; hP >= 0 && !found; hP--) {
            for (int wP = 0; wP < bmpInt.length; wP++) {
                if (bmpInt[wP][hP] != Color.rgb(105, 105, 105)) {
                    MBot = bmp.getHeight() - hP;
                    found = true;
                    break;
                }
            }
        }
        found = false;

        for (int wP = 0; wP < bmpInt.length && !found; wP++) {
            for (int hP = 0; hP < bmpInt[0].length; hP++) {
                if (bmpInt[wP][hP] != Color.rgb(105, 105, 105)) {
                    MLeft = wP;
                    found = true;
                    break;
                }
            }
        }
        found = false;

        for (int wP = bmpInt.length - 1; wP >= 0 && !found; wP--) {
            for (int hP = 0; hP < bmpInt[0].length; hP++) {
                if (bmpInt[wP][hP] != Color.rgb(105, 105, 105)) {
                    MRight = bmp.getWidth() - wP;
                    found = true;
                    break;
                }
            }

        }

        MTop = Math.max(MTop - 50, 0);
        MBot = Math.max(MBot - 50, 0);
        MLeft = Math.max(MLeft - 50, 0);
        MRight = Math.max(MRight - 50, 0);

        int sizeY = bmp.getHeight() - MBot - MTop, sizeX = bmp.getWidth() - MRight - MLeft;

        return Bitmap.createBitmap(bmp, MLeft, MTop, sizeX, sizeY);
    }

    private String doubleEscapeTeX(String s) {
        String t = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\'') t += '\\';
            if (s.charAt(i) != '\n') t += s.charAt(i);
            if (s.charAt(i) == '\\') t += "\\";
        }
        return t;
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void typesetDone() {
        typesetDone = true;
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void searchMathML(String mml) {
        try {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.wolframalpha.com/input/?i=" + URLEncoder.encode(mml, "UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
