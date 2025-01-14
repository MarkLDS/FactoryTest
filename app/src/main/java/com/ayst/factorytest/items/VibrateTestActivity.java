package com.ayst.factorytest.items;

import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import com.ayst.factorytest.R;
import com.ayst.factorytest.base.ChildTestActivity;

public class VibrateTestActivity extends ChildTestActivity {
    private Vibrator mVibrator = null;
    private final long VIBRATOR_ON_TIME = 1000;
    private final long VIBRATOR_OFF_TIME = 500;
    long[] pattern = {VIBRATOR_OFF_TIME, VIBRATOR_ON_TIME};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startVibrate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVibrator.cancel();
    }

    private void startVibrate() {
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (mVibrator == null) {
            Log.e( "VibrateTestActivity","No mVibrator here");
        }
        mVibrator.vibrate(pattern, 0);
    }

    @Override
    public int getContentLayout() {
        return R.layout.content_vibrate_test;
    }

    @Override
    public int getFullscreenLayout() {
        return 0;
    }

    @Override
    public void initViews() {
        super.initViews();
    }
}