package com.chupapp.distributor.helper.network;

/**
 * Created by inmobitec on 02/12/17.
 */


import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by inmobitec on 09/08/17.
 */

public class StringRequest extends Request<StringRequest.reply> {


    private Response.Listener<StringRequest.reply> ltn;

    public StringRequest(int method, String url, Response.Listener<StringRequest.reply> responseListener, Response.ErrorListener listener) {
        super(method, url, listener);
        this.ltn = responseListener;
    }

    @Override
    protected void deliverResponse(reply response) {
        this.ltn.onResponse(response);
    }

    @Override
    protected Response<reply> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }

        reply res = new reply();
        res.headers = response.headers;
        res.response = parsed;

        return Response.success(res, HttpHeaderParser.parseCacheHeaders(response));
    }


    public static class reply {
        public Map<String, String> headers;
        public String response;
    }

}
