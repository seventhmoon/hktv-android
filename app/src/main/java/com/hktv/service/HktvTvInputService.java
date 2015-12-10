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

package com.hktv.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;

import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.hktv.api.WebServiceManager;
import com.hktv.application.MainApplication;
import com.hktv.model.AccountTokenResponseModel;
import com.hktv.model.PlaylistRequestResponseModel;
import com.hktv.ott.R;

import java.io.IOException;


public class HktvTvInputService extends TvInputService {
    private static final String TAG = HktvTvInputService.class.getSimpleName();

    //    private String mToken;
    @Override
    public Session onCreateSession(String inputId) {
        return new SimpleSessionImpl(this);
    }

    //    private Helper.Channel[] channels = {Helper.Channel.HDJ, Helper.Channel.J2, Helper.Channel.INEWS};
    //    private Helper.Quality quality = Helper.Quality.QUALITY_720P;

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class SimpleSessionImpl extends Session {
        //        private static final int RESOURCE_1 =
        //                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_22050hz;
        //        private static final int RESOURCE_2 =
        //                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;

        private MediaPlayer mPlayer;
        private float mVolume;
        private Surface mSurface;

        SimpleSessionImpl(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
            if (mPlayer != null) {
                mPlayer.release();
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            if (mPlayer != null) {
                mPlayer.setSurface(surface);
            }
            mSurface = surface;
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            if (mPlayer != null) {
                mPlayer.setVolume(volume, volume);
            }
            mVolume = volume;
        }

        @Override
        public boolean onTune(Uri channelUri) {
            String[] projection = {TvContract.Channels.COLUMN_SERVICE_ID};
            //            int resource = RESOURCE_1;
            //            String url;

            final WebServiceManager hktvWebServiceManager = new WebServiceManager(getApplicationContext(), ((MainApplication) getApplication()).getRequestQueue());

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(channelUri, projection, null, null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    return false;
                }
                cursor.moveToNext();
                hktvWebServiceManager.getAccountToken(new Response.Listener<AccountTokenResponseModel>() {
                    @Override
                    public void onResponse(AccountTokenResponseModel response) {
                        String token = response.getToken();
                        hktvWebServiceManager.getPlaylist(1, 1, token, new Response.Listener<PlaylistRequestResponseModel>() {

                            @Override
                            public void onResponse(PlaylistRequestResponseModel response) {
                                String playlist = response.getPlaylist();
                                startPlayback(playlist);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(TAG, error.toString());
                                Toast.makeText(HktvTvInputService.this, R.string.text_network_error, Toast.LENGTH_SHORT).show();
                                //                                showErrorDialog(HktvTvInputService.this, getString(R.string.text_network_error));
                            }
                        });
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                        Toast.makeText(HktvTvInputService.this, R.string.text_network_error, Toast.LENGTH_SHORT).show();
                        //                        showErrorDialog(HktvTvInputService.this, getString(R.string.text_network_error));
                    }
                });

                //                new PreparePlaybackTask().execute(cursor.getInt(0));
                //                url = Helper.getPlaylist(channels[cursor.getInt(0)], quality);
                //                resource = (cursor.getInt(0) == TvbTvInputSetupActivity.CHANNEL_1_SERVICE_ID ?
                //                        RESOURCE_1 : RESOURCE_2);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            //            return startPlayback(url);
            return true;
            // NOTE: To display the program information (e.g. title) properly in the channel banner,
            // The implementation needs to register the program metadata on TvProvider.
            // For the example implementation, please see {@link RichTvInputService}.
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // The sample content does not have caption. Nothing to do in this sample input.
            // NOTE: If the channel has caption, the implementation should turn on/off the caption
            // based on {@code enabled}.
            // For the example implementation for the case, please see {@link RichTvInputService}.
        }

        private boolean startPlayback(String url) {
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer player, int what, int arg) {
                        // NOTE: TV input should notify the video playback state by using
                        // {@code notifyVideoAvailable()} and {@code notifyVideoUnavailable() so
                        // that the application can display back screen or spinner properly.
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                            return true;
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                                || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            notifyVideoAvailable();
                            return true;
                        }
                        return false;
                    }
                });
                mPlayer.setSurface(mSurface);
                mPlayer.setVolume(mVolume, mVolume);
            } else {
                mPlayer.reset();
            }
            mPlayer.setLooping(true);
            //            AssetFileDescriptor afd = getResources().openRawResourceFd(resource);
            //            if (afd == null) {
            //                return false;
            //            }
            try {
                mPlayer.setDataSource(url);
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        String msg = "";
                        if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                            msg = getString(R.string.video_error_media_load_timeout);
                        } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                            msg = getString(R.string.video_error_server_unaccessible);
                        } else {
                            msg = getString(R.string.video_error_unknown_error);
                        }
//                        mp.stop();
                        //                                                Toast.makeText(HktvTvInputService.this, msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                        //                        mPlayer.stopPlayback();
                        //                        mPlaybackState = PlaybackState.IDLE;
                        return false;
                        //                        Toast.makeText(HktvTvInputService.this, R.string.text_network_error, Toast.LENGTH_SHORT).show();
                        //                        showErrorDialog(HktvTvInputService.this, getString(R.string.text_network_error));
                        //                        return false;
                    }
                });
                //                mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                //                        afd.getDeclaredLength());
                mPlayer.prepareAsync();
                mPlayer.start();
            } catch (IOException e) {
                return false;
                //            } finally {
                //                try {
                //                    afd.close();
                //                } catch (IOException e) {
                //                    // Do nothing.
                //                }
            }
            // The sample content does not have rating information. Just allow the content here.
            // NOTE: If the content might include problematic scenes, it should not be allowed.
            // Also, if the content has rating information, the implementation should allow the
            // content based on the current rating settings by using
            // {@link android.media.tv.TvInputManager#isRatingBlocked()}.
            // For the example implementation for the case, please see {@link RichTvInputService}.
            notifyContentAllowed();
            return true;
        }


    }

    public static final void showErrorDialog(Context context, String errorString) {
        new AlertDialog.Builder(context).setTitle(R.string.error)
                .setMessage(errorString)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

}
