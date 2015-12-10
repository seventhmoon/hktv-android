package com.hktv.ui;

import android.app.FragmentManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaDrm;
import android.os.Build;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import com.hktv.ott.R;

import java.util.List;

/**
 * Created by fung.lam on 16/11/2015.
 */
public class InfoStepFragment extends GuidedStepFragment {
    private static final String URL_APK = "http://tv.hktvmall.com/programs/apk/HKTVlive.apk";
//    private MediaDrm mWidevineMediaDrm, mPlayreadyMediaDrm;

    @Override
    public int onProvideTheme() {
        return super.onProvideTheme();
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.app_name);
        String description = null;
        String breadcrumb = null;
        Drawable icon = null;

        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {

//        PackageInfo pInfo = null;
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);




            actions.add(new GuidedAction.Builder()
                    .infoOnly(true)
                    .title(getString(R.string.text_version))
                    .description(packageInfo.versionName)
                    .build());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }




    }



}

