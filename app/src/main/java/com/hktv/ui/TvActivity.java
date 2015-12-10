package com.hktv.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;

public class TvActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new InfoStepFragment(), android.R.id.content);
        }
    }
}
