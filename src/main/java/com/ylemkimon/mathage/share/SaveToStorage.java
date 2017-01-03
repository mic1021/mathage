package com.ylemkimon.mathage.share;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.ylemkimon.mathage.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class SaveToStorage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File dir = new File(Environment.getExternalStorageDirectory().toString() + "/math");
        dir.mkdirs();
        File dest = new File(dir, DateFormat.format("yyyy_MM_dd_HH_mm_ss", new Date()) + ".png");
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(new File(getCacheDir(), "temp.png"));
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
                output.close();
            } catch (Exception ignored) {
            }
        }
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{dest.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(final String path, Uri uri) {
                SaveToStorage.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SaveToStorage.this, getString(R.string.storage_copied) + path, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }
}
