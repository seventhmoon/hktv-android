package com.hktv.api;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import com.hktv.R;
import com.hktv.model.AccountTokenResponseModel;
import com.hktv.model.PlaylistRequestResponseModel;
import com.hktv.network.GsonObjectRequest;
import com.hktv.util.Utils;

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
    private RequestQueue mRequestQueue;

    public WebServiceManager(Context context, RequestQueue requestQueue) {
        mContext = context;
        mRequestQueue = requestQueue;
        mDeviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        mDevice = mContext.getString(R.string.param_device);
    }

    public void getAccountToken(Response.Listener<AccountTokenResponseModel> listener, Response.ErrorListener errorListener) {
        long timestamp = System.currentTimeMillis() / 1000l;
        long mUId = 0;
        String apiName = mContext.getString(R.string.api_account_token);
        String url = mContext.getString(R.string.api_host) + "/" + apiName;
        String signature = getSignature(apiName, timestamp, treeMap);
        
        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        treeMap.put("ki", mContext.getString(R.string.param_key_index));
        treeMap.put("muid", String.valueOf(mUId));
        treeMap.put("s", signature);
        treeMap.put("ts", String.valueOf(timestamp));

        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();

        for (String key : treeMap.keySet()) {
            obj.addProperty(key, treeMap.get(key));
        }

        GsonObjectRequest gsonReq = new GsonObjectRequest(Request.Method.POST,
                url, AccountTokenResponseModel.class, obj.toString(), listener, errorListener);

        // Adding request to request queue
        mRequestQueue.add(gsonReq);
    }

    public void getPlaylist(long userId, long videoId, String token, Response.Listener<PlaylistRequestResponseModel> listener, Response.ErrorListener errorListener) {
        long timestamp = System.currentTimeMillis() / 1000l;
        String apiName = mContext.getString(R.string.api_playlist_request);
        String url = mContext.getString(R.string.api_host) + "/" + apiName;
        String network = mContext.getString(R.string.param_network);
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL.toUpperCase();
        String release = Build.VERSION.RELEASE;
        String deviceId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        String device = mContext.getString(R.string.param_device);
        String signature = getSignature(apiName, timestamp, treeMap);
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
        treeMap.put("s", signature);
        treeMap.put("ts", String.valueOf(timestamp));

        String requestUrl = url + "?" + toUrlParams(treeMap);

        GsonObjectRequest gsonObjReq = new GsonObjectRequest(Request.Method.GET,
                requestUrl, PlaylistRequestResponseModel.class, null, listener, errorListener);

        // Adding request to request queue
        mRequestQueue.add(gsonObjReq);
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

        return md5String(plain);
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

    private String md5String(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

}
