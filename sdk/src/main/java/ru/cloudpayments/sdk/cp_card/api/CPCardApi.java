package ru.cloudpayments.sdk.cp_card.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONTokener;
import ru.cloudpayments.sdk.cp_card.api.models.BinInfo;

public class CPCardApi {

    public interface CompleteBinInfoListener {

        void onCompleted(final BinInfo binInfo);
    }

    public interface ErrorListener {

        void onError(final String message);
    }

    private final Context context;

    public CPCardApi(Context context) {
        this.context = context;
    }

    public void getBinInfo(String firstSixDigits, final CompleteBinInfoListener completeListener, final ErrorListener errorListener) {

        firstSixDigits = firstSixDigits.replace(" ", "");

        if (firstSixDigits.length() < 6) {
            errorListener.onError("You must specify the first 6 digits of the card number");
            return;
        }

        firstSixDigits = firstSixDigits.substring(0, 6);

        RequestQueue queue = Volley.newRequestQueue(context);
        String url ="https://widget.cloudpayments.ru/Home/BinInfo?firstSixDigits=" + firstSixDigits;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject jsonInfo = null;
                        try {
                            JSONObject jsonResp;
                            Object resp = new JSONTokener(response).nextValue();
                            if (resp instanceof JSONObject &&
                                (jsonResp = (JSONObject) resp).optBoolean("Success", false) &&
                                (jsonInfo = jsonResp.optJSONObject("Model")) != null) {
                                completeListener.onCompleted(new BinInfo(jsonInfo));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (jsonInfo == null) {
                            errorListener.onError("Unable to determine bank");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.onError(error.getMessage());
            }
        });

        queue.add(stringRequest);
    }
}
