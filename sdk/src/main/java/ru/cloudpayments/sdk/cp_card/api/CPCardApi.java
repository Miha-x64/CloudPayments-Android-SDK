package ru.cloudpayments.sdk.cp_card.api;

import android.content.Context;

import android.os.Handler;
import android.os.Looper;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import org.json.JSONObject;
import org.json.JSONTokener;
import ru.cloudpayments.sdk.cp_card.api.models.BinInfo;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executor;

public class CPCardApi {

    public interface CompleteBinInfoListener {

        void onCompleted(final BinInfo binInfo);
    }

    public interface ErrorListener {

        void onError(final String message);
    }

    private final Context context;

    /**
     * @param context may be null if you're not using {@link #getBinInfo(String, CompleteBinInfoListener, ErrorListener)}
     */
    public CPCardApi(Context context) {
        this.context = context;
    }

    /**
     * Get bin info using Volley.
     * @deprecated this method left for compatibility reasons. Use another overload to control cancellation
     */
    @Deprecated
    public void getBinInfo(
        String firstSixDigits, CompleteBinInfoListener completeListener, ErrorListener errorListener) {
        getBinInfo(Volley.newRequestQueue(context), firstSixDigits, completeListener, errorListener);
    }
    /** Get bin info using Volley. */
    public void getBinInfo(
        RequestQueue queue,
        String firstSixDigits, CompleteBinInfoListener completeListener, ErrorListener errorListener) {
        String url = buildUrl(firstSixDigits, errorListener);
        if (url == null) return;

        queue.add(new StringRequest(Method.GET, url, new Response.Listener<String>() {
            @Override public void onResponse(String response) {
                BinInfo binInfo = parse(response);
                if (binInfo != null) {
                    completeListener.onCompleted(binInfo);
                } else {
                    errorListener.onError("Unable to determine bank");
                }
            }
        }, new Response.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                errorListener.onError(error.getMessage());
            }
        }));
    }
    /** Get bin info using OkHttp.*/
    public Closeable getBinInfo(
        Call.Factory okHttp,
        String firstSixDigits, CompleteBinInfoListener completeListener, ErrorListener errorListener) {
        return getBinInfo(okHttp, new Executor() {
            @Override public void execute(Runnable runnable) {
                new Handler(Looper.getMainLooper()).post(runnable);
            }
        }, firstSixDigits, completeListener, errorListener);
    }
    /** Get bin info using OkHttp.*/
    public Closeable getBinInfo(
        Call.Factory okHttp, Executor resultExecutor,
        String firstSixDigits, CompleteBinInfoListener completeListener, ErrorListener errorListener) {
        String url = buildUrl(firstSixDigits, errorListener);
        if (url == null) return null;

        Call call = okHttp.newCall(new Request.Builder().url(url).build());
        call.enqueue(new Callback() {
            @Override public void onResponse(Call call, okhttp3.Response response) throws IOException {
                deliver(call, parse(response.body().string()), null);
            }
            @Override public void onFailure(Call call, IOException e) {
                deliver(call, null, e.getMessage());
            }
            private void deliver(Call call, BinInfo info, String error) {
                resultExecutor.execute(new Runnable() {
                    @Override public void run() {
                        if (!call.isCanceled()) {
                            if (info != null) {
                                completeListener.onCompleted(info);
                            } else {
                                errorListener.onError(error == null ? "Unable to determine bank" : error);
                            }
                        }
                    }
                });
            }
        });
        return new Closeable() {
            @Override public void close() {
                call.cancel();
            }
        };
    }

    private static final String URL = "https://widget.cloudpayments.ru/Home/BinInfo?firstSixDigits=";
    private String buildUrl(String firstSixDigits, ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(URL);
        char c;
        for (int i = 0, size = firstSixDigits.length(); i < size; i++)
            if ((c = firstSixDigits.charAt(i)) >= '0' && c <= '9')
                sb.append(c);

        if (sb.length() < URL.length() + 6) {
            errorListener.onError("You must specify the first 6 digits of the card number");
            return null;
        }

        return sb.toString();
    }

    private BinInfo parse(String response) {
        try {
            JSONObject jsonResp, jsonInfo;
            Object resp = new JSONTokener(response).nextValue();
            if (resp instanceof JSONObject &&
                (jsonResp = (JSONObject) resp).optBoolean("Success", false) &&
                (jsonInfo = jsonResp.optJSONObject("Model")) != null) {
                return new BinInfo(jsonInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
