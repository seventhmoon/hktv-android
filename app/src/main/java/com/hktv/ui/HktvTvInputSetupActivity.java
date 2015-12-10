/*
 * Copyright 2015 The Android Open Source Project
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

package com.hktv.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import com.hktv.ott.R;



public class HktvTvInputSetupActivity extends Activity {


    private static final String CHANNEL_1_NUMBER = "00";
    private static final String CHANNEL_1_NAME = "HKTV";
    private static final int CHANNEL_1_ORIG_NETWORK_ID = 0;
    private static final int CHANNEL_1_TRANSPORT_STREAM_ID = 0;
    public static final int CHANNEL_1_SERVICE_ID = 1;


    private String mInputId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        DialogFragment newFragment = new MyAlertDialogFragment();
        newFragment.show(getFragmentManager(), "dialog");
    }

    private void registerChannels() {
        // Check if we already registered channels.
        Uri uri = TvContract.buildChannelsUriForInput(mInputId);
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                return;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put(TvContract.Channels.COLUMN_INPUT_ID, mInputId);

        // Register channel 1-1.
        values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, CHANNEL_1_NUMBER);
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, CHANNEL_1_NAME);
        values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, CHANNEL_1_ORIG_NETWORK_ID);
        values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, CHANNEL_1_TRANSPORT_STREAM_ID);
        values.put(TvContract.Channels.COLUMN_SERVICE_ID, CHANNEL_1_SERVICE_ID);
        getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);


    }

    public static class MyAlertDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.hktv_setup_title)
                    .setMessage(R.string.simple_setup_message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((HktvTvInputSetupActivity) getActivity()).registerChannels();
                                    // Sets the results so that the application can process the
                                    // registered channels properly.
                                    getActivity().setResult(Activity.RESULT_OK);
                                    getActivity().finish();
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().finish();
                                }
                            }
                    )
                    .create();
        }
    }
}
