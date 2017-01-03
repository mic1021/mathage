package com.ylemkimon.mathage;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.viewpagerindicator.LinePageIndicator;
import com.ylemkimon.mathage.keyboard.MathKeyboardService;

import java.util.List;

public class TutorialActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {
    private PageAdapter mPageAdapter;
    private ViewPager mPager;

    private YouTubePlayer mFullscreenPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_tutorial);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(7680 / getResources().getDisplayMetrics().densityDpi);
        mPageAdapter = new PageAdapter(this.getSupportFragmentManager());
        mPager.setAdapter(mPageAdapter);

        LinePageIndicator indicator = (LinePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setOnPageChangeListener(this);
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
        if (mFullscreenPlayer != null)
            mFullscreenPlayer.setFullscreen(false);
        else
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        mPageAdapter.refeshPages(aEnabled, imEnabled);
        onPageSelected(mPager.getCurrentItem());
    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i < mPageAdapter.getCount(); i++) {
            TutorialFragment fragment = mPageAdapter.fragments.get(i);
            if (fragment != null) {
                if (i == position)
                    fragment.initializePlayer();
                else
                    fragment.releasePlayer();
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private static class PageAdapter extends FragmentStatePagerAdapter {
        public SparseArray<TutorialFragment> fragments = new SparseArray<>(4);

        private boolean aEnabled = false;
        private boolean imEnabled = false;

        public PageAdapter(FragmentManager fm) {
            super(fm);
        }

        public void refeshPages(boolean aEnabled, boolean imEnabled) {
            this.aEnabled = aEnabled;
            this.imEnabled = imEnabled;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            int realPosition = position;
            if (!aEnabled && imEnabled && position == 0)
                position = -1;
            TutorialFragment fragment = TutorialFragment.newInstance(position - getCount() + 4, realPosition);
            fragments.put(realPosition, fragment);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2 + (aEnabled ? 0 : 1) + (imEnabled ? 0 : 1);
        }

        @Override
        public float getPageWidth(int position) {
            return 0.94f;
        }

        @Override
        public int getItemPosition(Object object) {
            int position = POSITION_NONE;
            switch (((TutorialFragment) object).mNum) {
                case 0:
                    position = aEnabled ? POSITION_NONE : 0;
                    break;
                case 1:
                    position = imEnabled ? POSITION_NONE : (aEnabled ? 0 : 1);
                    break;
                case 2:
                    position = getCount() - 2;
                    break;
                case 3:
                    position = getCount() - 1;
                    break;
            }
            fragments.put(position, (TutorialFragment) object);
            return position;
        }
    }

    public static class TutorialFragment extends Fragment implements YouTubePlayer.OnInitializedListener {
        private static final String ARGUMENT_NUM = "num";
        private static final String ARGUMENT_POSITION = "position";

        public int mNum;
        public int mPosition; // used only for createview
        private YouTubePlayer mPlayer;

        public TutorialFragment() {
        }

        static TutorialFragment newInstance(int num, int position) {
            TutorialFragment f = new TutorialFragment();
            Bundle args = new Bundle();
            args.putInt(ARGUMENT_NUM, num);
            args.putInt(ARGUMENT_POSITION, position);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt(ARGUMENT_NUM) : 0;
            mPosition = getArguments() != null ? getArguments().getInt(ARGUMENT_POSITION) : 0;
        }

        public void initializePlayer() {
            if (mNum < 2) {
                YouTubePlayerSupportFragment youTubePlayerFragment = (YouTubePlayerSupportFragment) getChildFragmentManager().findFragmentById(R.id.youtube_fragment);
                youTubePlayerFragment.initialize("AIzaSyA70sfaF3kqD1MIFo5GS8D2RoVpI6STTkw", this);
            }
        }

        public void releasePlayer() {
            if (mPlayer != null)
                mPlayer.release();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v;
            if (mNum < 2) {
                v = inflater.inflate(R.layout.fragment_tutorial_youtube, container, false);

                if (mPosition == 0)
                    initializePlayer();

                TextView textView = (TextView) v.findViewById(R.id.textView);
                textView.setText(getResources().getStringArray(R.array.tutorial)[mNum]);
            } else {
                v = inflater.inflate(R.layout.fragment_tutorial_image, container, false);

                ImageView imageView = (ImageView) v.findViewById(R.id.imageView);
                if (mNum == 2)
                    imageView.setImageResource(R.drawable.tutorial_main);
                else //if (mNum == 3)
                    imageView.setImageResource(R.drawable.tutorial_vk);
            }

            Button button = (Button) v.findViewById(R.id.button);
            switch (mNum) {
                case 0:
                    button.setText(R.string.tutorial_open_acc);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        }
                    });
                    break;
                case 1:
                    button.setText(R.string.tutorial_open_im);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                        }
                    });
                    break;
                case 3:
                    button.setText(android.R.string.ok);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().onBackPressed();
                        }
                    });
                    break;
                default:
                    button.setVisibility(View.GONE);
                    break;
            }

            return v;
        }

        @Override
        public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
            if (!wasRestored) {
                mPlayer = player;
                player.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
                    @Override
                    public void onFullscreen(boolean full) {
                        ((TutorialActivity) getActivity()).mFullscreenPlayer = full ? mPlayer : null;
                    }
                });
                switch (mNum) {
                    case 0:
                        player.cueVideo("iROVPz64duw");
                        break;
                    case 1:
                        player.cueVideo("7XzMGiVXTOk");
                        break;
                }
            }
        }

        @Override
        public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        }
    }
}
