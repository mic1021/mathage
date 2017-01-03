package com.ylemkimon.mathage.share;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.ylemkimon.mathage.R;

public class CopyToClipboard extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("mathAge", getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT)));
        Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
        finish();
    }
}
