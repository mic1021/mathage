package com.ylemkimon.mathage.settings;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.ylemkimon.mathage.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsAppListActivity extends ListActivity {
    private PackageManager mPackageManager;
    private List<ApplicationInfo> mList = new ArrayList<>();
    private ApplicationInfoAdapter mAdaptor;

    private boolean mServiceNeedsRestart = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_list);
        mPackageManager = getPackageManager();
        new LoadApplications().execute();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mServiceNeedsRestart)
            android.os.Process.killProcess(Process.myPid());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mServiceNeedsRestart) {
            mServiceNeedsRestart = false;
            PendingIntent pi = PendingIntent.getActivity(this, 1, new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtras(getIntent()), PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
            Process.killProcess(Process.myPid());
        } else
            super.onBackPressed();
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            List<ApplicationInfo> list = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : list) {
                if (mPackageManager.getLaunchIntentForPackage(info.packageName) != null)
                    mList.add(info);
            }
            Collections.sort(mList, new ApplicationInfo.DisplayNameComparator(mPackageManager));
            mAdaptor = new ApplicationInfoAdapter(SettingsAppListActivity.this, mList);
            return null;
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(SettingsAppListActivity.this, null, getString(R.string.please_wait));
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(mAdaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }
    }

    private class ApplicationInfoAdapter extends ArrayAdapter<ApplicationInfo> {
        private List<ApplicationInfo> mList = null;
        private Set<String> mAppsList = null;
        private Context mContext;
        private PackageManager packageManager;

        public ApplicationInfoAdapter(Context context, List<ApplicationInfo> appsList) {
            super(context, R.layout.row_app_list_row, appsList);
            mContext = context;
            mList = appsList;
            packageManager = context.getPackageManager();
            final SettingsManager sm = new SettingsManager(mContext);
            mAppsList = new HashSet<>(sm.getAppList());
        }

        @Override
        public int getCount() {
            return mList != null ? mList.size() : 0;
        }

        @Override
        public ApplicationInfo getItem(int position) {
            return mList != null ? mList.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.row_app_list_row, parent, false);

                CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkBox);
                checkbox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        ApplicationInfo applicationInfo = (ApplicationInfo) cb.getTag();
                        if (cb.isChecked())
                            mAppsList.add(applicationInfo.packageName);
                        else
                            mAppsList.remove(applicationInfo.packageName);
                        final SettingsManager sm = new SettingsManager(mContext);
                        sm.setAppList(mAppsList);
                        mServiceNeedsRestart = true;
                    }
                });
            }

            ApplicationInfo applicationInfo = mList.get(position);
            if (null != applicationInfo) {
                TextView appName = (TextView) view.findViewById(R.id.app_name);
                TextView packageName = (TextView) view.findViewById(R.id.app_paackage);
                ImageView iconview = (ImageView) view.findViewById(R.id.app_icon);
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkBox);

                appName.setText(applicationInfo.loadLabel(packageManager));
                packageName.setText(applicationInfo.packageName);
                iconview.setImageDrawable(applicationInfo.loadIcon(packageManager));
                checkbox.setChecked(mAppsList.contains(applicationInfo.packageName));
                checkbox.setTag(applicationInfo);
            }
            return view;
        }
    }
}