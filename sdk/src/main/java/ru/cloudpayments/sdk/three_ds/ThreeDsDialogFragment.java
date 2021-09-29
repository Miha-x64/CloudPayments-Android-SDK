package ru.cloudpayments.sdk.three_ds;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.fragment.app.DialogFragment;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ThreeDsDialogFragment extends DialogFragment {

    static final String POST_BACK_URL = "https://demo.cloudpayments.ru/WebFormPost/GetWebViewData";

    private ThreeDSDialogListener listener;

    public static ThreeDsDialogFragment newInstance(String acsUrl, String md, String paReq) {
        ThreeDsDialogFragment dialogFragment = new ThreeDsDialogFragment();
        Bundle args = new Bundle(1);
        args.putStringArray("args", new String[]{ acsUrl, md, paReq });
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    static WebView view(Activity activity, String acsUrl, String md, String paReq, ThreeDSDialogListener listener) {
        WebView webViewThreeDs = new WebView(activity);
        webViewThreeDs.setWebViewClient(new ThreeDsDialogFragment.ThreeDsWebViewClient(activity, listener, md));
        webViewThreeDs.getSettings().setDomStorageEnabled(true);
        webViewThreeDs.getSettings().setJavaScriptEnabled(true);
        webViewThreeDs.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        try {
            String params = new StringBuilder()
                    .append("PaReq=").append(URLEncoder.encode(paReq, "UTF-8"))
                    .append("&MD=").append(URLEncoder.encode(md, "UTF-8"))
                    .append("&TermUrl=").append(URLEncoder.encode(POST_BACK_URL, "UTF-8"))
                    .toString();
            webViewThreeDs.postUrl(acsUrl, params.getBytes());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return webViewThreeDs;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] args = requireArguments().getStringArray("args");
        return ThreeDs.dialog(requireActivity(), args[0], args[1], args[2], listener);
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    static final class ThreeDsWebViewClient extends WebViewClient {
        private final Activity activity;
        private final ThreeDSDialogListener listener;
        private final String md;
        ThreeDsWebViewClient(Activity activity, ThreeDSDialogListener listener, String md) {
            this.activity = activity;
            this.listener = listener;
            this.md = md;
        }

        @Override public void onPageFinished(WebView view, String url) {
            if (url.equalsIgnoreCase(POST_BACK_URL)) {
                view.setVisibility(View.GONE);
                eval(view, "JSON.parse(document.getElementsByTagName('body')[0].innerText).PaRes",
                    new ValueCallback<String>() {
                        @Override public void onReceiveValue(String paRes) {
                            if (listener != null) {
                                if (paRes == null) {
                                    eval(view, "document.getElementsByTagName('html')[0].innerHTML",
                                        new ValueCallback<String>() {
                                            @Override public void onReceiveValue(String s) {
                                                listener.onAuthorizationFailed(s);
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
                    activity.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            callback.onReceiveValue(finalUnquoted);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ThreeDSDialogListener) {
            listener = (ThreeDSDialogListener) context;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (activity instanceof ThreeDSDialogListener) {
                listener = (ThreeDSDialogListener) activity;
            }
        }
    }
}
