package com.hktv.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class HktvLiveChannelPlayerActivity extends Activity {

    private static final String TAG = "PlayerActivity";

    private static final int HIDE_CONTROLLER_TIME = 5000;
    private static final int SEEKBAR_DELAY_TIME = 100;
    private static final int SEEKBAR_INTERVAL_TIME = 1000;
    private static final int MIN_SCRUB_TIME = 3000;
    private static final int SCRUB_SEGMENT_DIVISOR = 30;
    private static final double MEDIA_BAR_TOP_MARGIN = 0.8;
    private static final double MEDIA_BAR_RIGHT_MARGIN = 0.2;
    private static final double MEDIA_BAR_BOTTOM_MARGIN = 0.0;
    private static final double MEDIA_BAR_LEFT_MARGIN = 0.2;
    private static final double MEDIA_BAR_HEIGHT = 0.1;
    private static final double MEDIA_BAR_WIDTH = 0.9;
    private final Handler mHandler = new Handler();
    private VideoView mVideoView;
    private TextView mStartText;
    private TextView mEndText;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private ProgressBar mLoading;
    private View mControllers;
    private View mContainer;
    private Timer mSeekbarTimer;
    private Timer mControllersTimer;
    private PlaybackState mPlaybackState;
    private Movie mSelectedMovie;
    private boolean mShouldStartPlayback;
    private boolean mControlersVisible;
    View.OnClickListener mPlayPauseHandler = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d(TAG, "clicked play pause button");

            if (!mControlersVisible) {
                updateControlersVisibility(true);
            }

            if (mPlaybackState == PlaybackState.PAUSED) {
                mPlaybackState = PlaybackState.PLAYING;
                updatePlayButton(mPlaybackState);
                mVideoView.start();
                startControllersTimer();
            } else {
                mVideoView.pause();
                mPlaybackState = PlaybackState.PAUSED;
                updatePlayButton(PlaybackState.PAUSED);
                stopControllersTimer();
            }
        }
    };
    private int mDuration;
    private DisplayMetrics mMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        loadViews();
        setupController();
        setupControlsCallbacks();
        preparePlaylist();
        updateMetadata(true);
    }

    private void playPlaylist(String hlsPlaylist) {

        if (null != hlsPlaylist) {
            mShouldStartPlayback = true; //b.getBoolean(getResources().getString(R.string.should_start));
            int startPosition = 0; //b.getInt(getResources().getString(R.string.start_position), 0);

            mVideoView.setVideoPath(hlsPlaylist);

            if (mShouldStartPlayback) {
                mPlaybackState = PlaybackState.PLAYING;
                updatePlayButton(mPlaybackState);
                if (startPosition > 0) {
                    mVideoView.seekTo(startPosition);
                }
                mVideoView.start();
                mPlayPause.requestFocus();
                startControllersTimer();
            } else {
                updatePlaybackLocation();
                mPlaybackState = PlaybackState.PAUSED;
                updatePlayButton(mPlaybackState);
            }
        }
    }


    private void startVideoPlayer() {
        Bundle b = getIntent().getExtras();
        mSelectedMovie = (Movie) getIntent().getSerializableExtra(getResources().getString(R.string.movie));
        if (null != b) {
            mShouldStartPlayback = b.getBoolean(getResources().getString(R.string.should_start));
            int startPosition = b.getInt(getResources().getString(R.string.start_position), 0);
            mVideoView.setVideoPath(mSelectedMovie.getVideoUrl());
            //modified by fung

//            mVideoView.setVideoPath(this.mPlaylist);

            if (mShouldStartPlayback) {
                mPlaybackState = PlaybackState.PLAYING;
                updatePlayButton(mPlaybackState);
                if (startPosition > 0) {
                    mVideoView.seekTo(startPosition);
                }
                mVideoView.start();
                mPlayPause.requestFocus();
                startControllersTimer();
            } else {
                updatePlaybackLocation();
                mPlaybackState = PlaybackState.PAUSED;
                updatePlayButton(mPlaybackState);
            }
        }
    }

    private void updatePlaybackLocation() {
        if (mPlaybackState == PlaybackState.PLAYING ||
                mPlaybackState == PlaybackState.BUFFERING) {
            startControllersTimer();
        } else {
            stopControllersTimer();
        }
    }

    private void play(int position) {
        startControllersTimer();
        mVideoView.seekTo(position);
        mVideoView.start();
        restartSeekBarTimer();
    }

    private void stopSeekBarTimer() {
        Log.d(TAG, "Stopped TrickPlay Timer");
        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartSeekBarTimer() {
        stopSeekBarTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), SEEKBAR_DELAY_TIME,
                SEEKBAR_INTERVAL_TIME);
    }

    private void stopControllersTimer() {
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), HIDE_CONTROLLER_TIME);
    }

    private void updateControlersVisibility(boolean show) {
        if (show) {
            mControllers.setVisibility(View.VISIBLE);
        } else {
            mControllers.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() was called");
        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
            mSeekbarTimer = null;
        }
        if (null != mControllersTimer) {
            mControllersTimer.cancel();
        }
        mVideoView.pause();
        mPlaybackState = PlaybackState.PAUSED;
        updatePlayButton(PlaybackState.PAUSED);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() was called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() is called");
        stopControllersTimer();
        stopSeekBarTimer();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart was called");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        super.onResume();
    }

    private void setupController() {

        int w = (int) (mMetrics.widthPixels * MEDIA_BAR_WIDTH);
        int h = (int) (mMetrics.heightPixels * MEDIA_BAR_HEIGHT);
        int marginLeft = (int) (mMetrics.widthPixels * MEDIA_BAR_LEFT_MARGIN);
        int marginTop = (int) (mMetrics.heightPixels * MEDIA_BAR_TOP_MARGIN);
        int marginRight = (int) (mMetrics.widthPixels * MEDIA_BAR_RIGHT_MARGIN);
        int marginBottom = (int) (mMetrics.heightPixels * MEDIA_BAR_BOTTOM_MARGIN);
        LayoutParams lp = new LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        mControllers.setLayoutParams(lp);
        mStartText.setText(getResources().getString(R.string.init_text));
        mEndText.setText(getResources().getString(R.string.init_text));
    }

    private void setupControlsCallbacks() {

        mVideoView.setOnErrorListener(new OnErrorListener() {

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
                mVideoView.stopPlayback();
                mPlaybackState = PlaybackState.IDLE;
                return false;
            }
        });

        mVideoView.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "onPrepared is reached");
                mDuration = mp.getDuration();
                mEndText.setText(Utils.formatMillis(mDuration));
                mSeekbar.setMax(mDuration);
                restartSeekBarTimer();
            }
        });

        mVideoView.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                stopSeekBarTimer();
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(PlaybackState.IDLE);
                mControllersTimer = new Timer();
                mControllersTimer.schedule(new BackToDetailTask(), HIDE_CONTROLLER_TIME);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int currentPos = 0;
        int delta = (int) (mDuration / SCRUB_SEGMENT_DIVISOR);
        if (delta < MIN_SCRUB_TIME)
            delta = MIN_SCRUB_TIME;

        Log.v("keycode", "duration " + mDuration + " delta:" + delta);
        if (!mControlersVisible) {
            updateControlersVisibility(true);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                currentPos = mVideoView.getCurrentPosition();
                currentPos -= delta;
                if (currentPos > 0)
                    play(currentPos);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                currentPos = mVideoView.getCurrentPosition();
                currentPos += delta;
                if (currentPos < mDuration)
                    play(currentPos);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(Utils.formatMillis(position));
        mEndText.setText(Utils.formatMillis(duration));
    }

    private void updatePlayButton(PlaybackState state) {
        switch (state) {
            case PLAYING:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_pause_playcontrol_normal));
                break;
            case PAUSED:
            case IDLE:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_play_playcontrol_normal));
                break;
            case BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /*
     * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { return
     * super.onKeyDown(keyCode, event); }
     */

    private void updateMetadata(boolean visible) {
        mVideoView.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mStartText = (TextView) findViewById(R.id.startText);
        mEndText = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mPlayPause = (ImageView) findViewById(R.id.playpause);
        mLoading = (ProgressBar) findViewById(R.id.progressBar);
        mControllers = findViewById(R.id.controllers);
        mContainer = findViewById(R.id.container);

        mVideoView.setOnClickListener(mPlayPauseHandler);
    }

    private String getSignature(String apiName, long timestamp, TreeMap<String, String> map) {
        StringBuffer sb = new StringBuffer();
        sb.append(apiName);
        for (String param : map.values()) {
            sb.append(param);
        }
        sb.append(getString(R.string.param_secret_key));
        sb.append(timestamp);

        String plain = sb.toString();

        Log.d("TAG", plain);

        return new String(Hex.encodeHex(DigestUtils.md5(plain)));
    }

    private String toUrlParams(Map<String, String> params) {
        StringBuffer sb = new StringBuffer();
        for (String key : params.keySet()) {
            try {
                sb.append("&" + key + "=" + URLEncoder.encode(params.get(key), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString().substring(1);
    }

    private String getSignature(String apiName, long timestamp, String... params) {
        StringBuffer sb = new StringBuffer();
        sb.append(apiName);
        for (String param : params) {
            sb.append(param);
        }
        sb.append(getString(R.string.param_secret_key));
        sb.append(timestamp);

        String plain = sb.toString();

        Log.d("TAG", plain);

        return new String(Hex.encodeHex(DigestUtils.md5(plain)));
    }

    private void preparePlaylist() {


        String apiName = getString(R.string.api_account_token);
        String url = getString(R.string.api_host) + "/" + apiName;
        long timestamp = System.currentTimeMillis() / 1000l;
        String signature = getSignature("account/token", timestamp, getString(R.string.param_key_index), "0");

        JSONObject obj = new JSONObject();
        try {
            obj.put("muid", "0");
            obj.put("ts", String.valueOf(timestamp));
            obj.put("ki", getString(R.string.param_key_index));
            obj.put("s", signature);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("TAG", obj.toString());

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, obj,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());

                        try {
                            String token = response.getString("token");
                            getPlaylist(token);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.getMessage());

            }
        }) {


        };

// Adding request to request queue
        MainApplication.getInstance().addToRequestQueue(jsonObjReq, apiName);
    }

    private void getPlaylist(String token) {
        String apiName = getString(R.string.api_playlist_request);
        String url = getString(R.string.api_host) + "/" + apiName;

        long timestamp = System.currentTimeMillis() / 1000l;
        String network = getString(R.string.param_network);
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL.toUpperCase();
        String release = Build.VERSION.RELEASE;
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String device = getString(R.string.param_device);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        int resolutionMax = Math.min(screenHeight, screenWidth);

        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        treeMap.put("uid", "1");
        treeMap.put("vid", "1");
        treeMap.put("d", device);
        treeMap.put("mf", manufacturer);
        treeMap.put("mdl", model);
        treeMap.put("os", release);
        treeMap.put("udid", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        treeMap.put("mxres", String.valueOf(resolutionMax));
        treeMap.put("net", network);
        treeMap.put("t", token);
        treeMap.put("ki", getString(R.string.param_key_index));

        String signature = getSignature(apiName, timestamp, treeMap);
        treeMap.put("s", signature);
        treeMap.put("ts", String.valueOf(timestamp));

//        final Map<String, String> paramMap = new HashMap<String, String>(treeMap);

        Log.d(TAG, treeMap.toString());

        String requestUrl = url + "?" + toUrlParams(treeMap);
        Log.d(TAG, requestUrl);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                requestUrl, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        try {
                            String playlist = response.getString("m3u8");
                            playPlaylist(playlist);
//                            mSelectedMovie = (Movie) getIntent().getSerializableExtra(getResources().getString(R.string.movie));
//                            mSelectedMovie.setVideoUrl(playlist);
//                            startVideoPlayer();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


//                        mTextViewA.setText(response.toString());

//                        try {
////                            String token = response.getString("token");
//
//
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }

//                        pDialog.hide();
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
//                mTextViewA.setText(error.getMessage());
//                pDialog.hide();
            }
        }) {


        };

// Adding request to request queue
        MainApplication.getInstance().addToRequestQueue(jsonObjReq, apiName);
    }

    /*
     * List of various states that we can be in
     */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

    private class HideControllersTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControlersVisibility(false);
                    mControlersVisible = false;
                }
            });

        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos = 0;
                    currentPos = mVideoView.getCurrentPosition();
                    updateSeekbar(currentPos, mDuration);
                }
            });
        }
    }

    private class BackToDetailTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(HktvLiveChannelPlayerActivity.this, DetailsActivity.class);
                    intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);
                    startActivity(intent);
                }
            });

        }
    }


}

////////
