package com.hktv.api;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;

import com.android.volley.Request;
import com.android.volley.Response;
import com.hktv.android.MainApplication;
import com.hktv.android.R;
import com.hktv.android.Utils;
import com.hktv.android.network.GsonObjectRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by seventhmoon on 04/12/2014.
 */
public class WebServiceManager {

    public final String TAG = WebServiceManager.class.getSimpleName();
    private final String MANUFACTURER = Build.MANUFACTURER.toUpperCase();
    private final String MODEL = Build.MODEL.toUpperCase();
    private final String RELEASE = Build.VERSION.RELEASE;
    private Context mContext;
    private String mDeviceId;// = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    private String mDevice;// = mContext.getString(R.string.param_device);


    public WebServiceManager(Context context) {
        this.mContext = context;

        mDeviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        mDevice = mContext.getString(R.string.param_device);
    }


    public void getAccountToken(Response.Listener<AccountTokenResponseModel> listener, Response.ErrorListener errorListener) {


        String apiName = mContext.getString(R.string.api_account_token);
        String url = mContext.getString(R.string.api_host) + "/" + apiName;
        long timestamp = System.currentTimeMillis() / 1000l;
        long mUId = 0;
        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        treeMap.put("muid", String.valueOf(mUId));
        treeMap.put("ki", mContext.getString(R.string.param_key_index));
        String signature = getSignature(apiName, timestamp, treeMap);
        treeMap.put("s", signature);
        treeMap.put("ts", String.valueOf(timestamp));

        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();



        for (String key : treeMap.keySet()) {

            obj.addProperty(key, treeMap.get(key));
        }

        GsonObjectRequest gsonReq = new GsonObjectRequest(Request.Method.POST,
                url, AccountTokenResponseModel.class, obj.toString(), listener, errorListener);

// Adding request to request queue
        MainApplication.getInstance().addToRequestQueue(gsonReq, apiName);
    }

    public void getPlaylist(long userId, long videoId, String token, Response.Listener<PlaylistRequestResponseModel> listener, Response.ErrorListener errorListener) {
        String apiName = mContext.getString(R.string.api_playlist_request);
        String url = mContext.getString(R.string.api_host) + "/" + apiName;

        long timestamp = System.currentTimeMillis() / 1000l;
        String network = mContext.getString(R.string.param_network);
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL.toUpperCase();
        String release = Build.VERSION.RELEASE;
        String deviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        String device = mContext.getString(R.string.param_device);

        Point point = Utils.getDisplaySize(mContext);
        int resolutionMax = Math.min(point.x, point.y);

        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        treeMap.put("uid", String.valueOf(userId));
        treeMap.put("vid", String.valueOf(videoId));
        treeMap.put("d", device);
        treeMap.put("mf", manufacturer);
        treeMap.put("mdl", model);
        treeMap.put("os", release);
        treeMap.put("udid", Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        treeMap.put("mxres", String.valueOf(resolutionMax));
        treeMap.put("net", network);
        treeMap.put("t", token);
        treeMap.put("ki", mContext.getString(R.string.param_key_index));

        String signature = getSignature(apiName, timestamp, treeMap);
        treeMap.put("s", signature);
        treeMap.put("ts", String.valueOf(timestamp));


        String requestUrl = url + "?" + toUrlParams(treeMap);


        GsonObjectRequest gsonObjReq = new GsonObjectRequest(Request.Method.GET,
                requestUrl, PlaylistRequestResponseModel.class, null, listener, errorListener);

// Adding request to request queue
        MainApplication.getInstance().addToRequestQueue(gsonObjReq, apiName);
    }

    private String getSignature(String apiName, long timestamp, TreeMap<String, String> map) {
        StringBuffer sb = new StringBuffer();
        sb.append(apiName);
        for (String param : map.values()) {
            sb.append(param);
        }
        sb.append(mContext.getString(R.string.param_secret_key));
        sb.append(timestamp);

        String plain = sb.toString();


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

}
