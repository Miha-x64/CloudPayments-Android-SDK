package ru.cloudpayments.sdk.three_ds;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import androidx.annotation.CallSuper;
import androidx.fragment.app.Fragment;
import org.json.JSONException;
import org.json.JSONTokener;

import static android.net.Uri.encode;


public final class ThreeDs {
    private ThreeDs() {}

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView view(Context context) {
        WebView webViewThreeDs = new WebView(context);
        webViewThreeDs.getSettings().setDomStorageEnabled(true);
        webViewThreeDs.getSettings().setJavaScriptEnabled(true);
        webViewThreeDs.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        return webViewThreeDs;
    }

    private static final byte[] PaReq = "PaReq=".getBytes();
    private static final byte[] AndMd = "&MD=".getBytes();
    private static final byte[] AndTermUrl = "&TermUrl=".getBytes();
    public static byte[] requestBody(String paReq, String md, String termUrl) {
        byte[] paReqVal = encode(paReq).getBytes();
        byte[] mdVal = encode(md).getBytes();
        byte[] termUrlVal = encode(termUrl).getBytes();
        byte[] params = new byte[
            PaReq.length + paReqVal.length + AndMd.length + mdVal.length + AndTermUrl.length + termUrlVal.length];
        int pos = 0;
        pos = append(params, pos, PaReq);
        pos = append(params, pos, paReqVal);
        pos = append(params, pos, AndMd);
        pos = append(params, pos, mdVal);
        pos = append(params, pos, AndTermUrl);
        pos = append(params, pos, termUrlVal);
        return params;
    }
    private static int append(byte[] dst, int dstPos, byte[] src) {
        System.arraycopy(src, 0, dst, dstPos, src.length);
        return dstPos + src.length;
    }

    public static class WebViewClient extends android.webkit.WebViewClient {
        private Activity activity;
        private ThreeDSDialogListener listener;
        private final String md;
        private final String termUrl;
        public WebViewClient(Activity activity, ThreeDSDialogListener listener, String md, String termUrl) {
            this.activity = activity;
            this.listener = listener;
            this.md = md;
            this.termUrl = termUrl;
        }

        @CallSuper @Override public void onPageFinished(WebView view, String url) {
            if (url.equalsIgnoreCase(termUrl)) {
                view.setVisibility(View.GONE);
                eval(view, "JSON.parse(document.getElementsByTagName('body')[0].innerText).PaRes",
                    new ValueCallback<String>() {
                        @Override public void onReceiveValue(String paRes) {
                            if (listener != null) {
                                if (paRes == null) {
                                    eval(view, "document.getElementsByTagName('html')[0].innerHTML",
                                        new ValueCallback<String>() {
                                            @Override public void onReceiveValue(String s) {
                                                if (listener != null) {
                                                    listener.onAuthorizationFailed(s);
                                                }
                                            }
                                        }
                                    );
                                } else {
                                    listener.onAuthorizationCompleted(md, paRes);
                                }
                            }
                        }
                    }
                );
            }
        }
        private void eval(WebView view, String script, ValueCallback<String> callback) {
            view.evaluateJavascript(script, new ValueCallback<String>() {
                @Override public void onReceiveValue(final String jsString) {
                    String unquoted = null;
                    try {
                        Object parsed;
                        if (jsString != null && !jsString.isEmpty() &&
                            ((parsed = new JSONTokener(jsString).nextValue()) instanceof String) &&
                            !((String) parsed).isEmpty())
                            unquoted = (String) parsed;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String finalUnquoted = unquoted;
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                callback.onReceiveValue(finalUnquoted);
                            }
                        });
                    }
                }
            });
        }
        public void dispose() {
            activity = null;
            listener = null;
        }
    }

    public static final class DialogFragmentResultListener implements ThreeDSDialogListener {
        private final DialogInterface dialog;
        private final Fragment fragment;
        private final ThreeDSDialogListener delegate;
        public DialogFragmentResultListener(DialogInterface dialog, Fragment fragment, ThreeDSDialogListener delegate) {
            this.dialog = dialog;
            this.fragment = fragment;
            this.delegate = delegate;
        }

        @Override public void onAuthorizationCompleted(String md, String paRes) {
            String rk;
            if (fragment != null && (rk = fragment.requireArguments().getString("rk")) != null)
                deliver(rk, md, paRes, null);

            if (delegate != null)
                delegate.onAuthorizationCompleted(md, paRes);

            if (dialog != null)
                dialog.dismiss();
        }
        @Override public void onAuthorizationFailed(String html) {
            String rk;
            if (fragment != null && (rk = fragment.requireArguments().getString("rk")) != null)
                deliver(rk, null, null, html == null || html.length() > 128 * 1024 ? null : html);
            //                                        just a safe guess ^^^^^^^^^^
            if (delegate != null)
                delegate.onAuthorizationFailed(html);

            if (dialog != null)
                dialog.dismiss();
        }
        private void deliver(String rk, String md, String paRes, String html) {
            Bundle bun = new Bundle(2);
            if (md != null) {
                bun.putString("md", md);
                bun.putString("paRes", paRes);
            } else if (html != null) {
                bun.putString("html", html);
            } // else error HTML was truncated

            Bundle args = fragment.requireArguments();
            if (fragment.isAdded()) fragment.getParentFragmentManager().setFragmentResult(rk, bun);
            else args.putBundle("result", bun);
        }

        public static boolean tryDeliver(Fragment who) {
            Bundle args = who.requireArguments();
            Bundle result = args.getBundle("result");
            boolean deliver = result != null;
            if (deliver) who.getParentFragmentManager().setFragmentResult(args.getString("rk"), result);
            return deliver;
        }
    }

}
