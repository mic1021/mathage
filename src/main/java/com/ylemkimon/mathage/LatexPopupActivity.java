/*
* Copyright 2013 Luke Klinker
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ylemkimon.mathage;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

import org.apache.commons.lang3.StringEscapeUtils;

public class LatexPopupActivity extends Activity {
    public static final String EXTRA_LATEX = "LATEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.9f;
        params.dimAmount = 0.1f;
        getWindow().setAttributes(params);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .7));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

        setContentView(R.layout.activity_popup);
        WebView w = (WebView) findViewById(R.id.webView);
        w.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        w.getSettings().setJavaScriptEnabled(true);
        w.loadDataWithBaseURL("http://bar/",
                "<script type='text/x-mathjax-config'>MathJax.Hub.Config({showMathMenu: false, messageStyle: 'none', jax: ['input/TeX','output/HTML-CSS'], extensions: ['tex2jax.js'], TeX:{extensions: ['AMSmath.js','AMSsymbols.js','noErrors.js','noUndefined.js']}});</script><script type='text/javascript' src='http://cdn.mathjax.org/mathjax/latest/MathJax.js'></script>"
                        + StringEscapeUtils.escapeHtml4(getIntent().getStringExtra(EXTRA_LATEX)).replaceAll("(\r\n|\n)", "<br />"),
                null, "utf-8", null);
    }
}
