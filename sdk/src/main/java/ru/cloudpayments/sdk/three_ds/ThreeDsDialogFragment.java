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

    @Deprecated public ThreeDsDialogFragment() {
    }

    public ThreeDsDialogFragment(String acsUrl, String md, String paReq, String requestKey) {
        Bundle args = new Bundle(2);
        args.putStringArray("args", new String[]{ acsUrl, md, paReq });
        args.putString("rk", requestKey);
        setArguments(args);
    }

    public static ThreeDsDialogFragment newInstance(String acsUrl, String md, String paReq) {
        return new ThreeDsDialogFragment(acsUrl, md, paReq, null);
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
        Bundle bun = requireArguments();
        String[] args = bun.getStringArray("args");
        String rk = bun.getString("rk");
        return ThreeDs.dialog(
            requireActivity(),
            args[0], args[1], args[2],
            rk == null ? listener : new ThreeDSDialogListener() {
                @Override public void onAuthorizationCompleted(String md, String paRes) {
                    deliver(md, paRes, null);
                    if (listener != null) listener.onAuthorizationCompleted(md, paRes);
                }
                @Override public void onAuthorizationFailed(String html) {
                    deliver(null, null, html == null || html.length() > 128 * 1024 ? null : html);
                    //                                just a safe guess ^^^^^^^^^^
                    if (listener != null) listener.onAuthorizationFailed(html);
                }
                private void deliver(String md, String paRes, String html) {
                    Bundle bun = new Bundle(2);
                    if (md != null) {
                        bun.putString("md", md);
                        bun.putString("paRes", paRes);
                    } else if (html != null) {
                        bun.putString("html", html);
                    } // else error HTML was truncated

                    if (isAdded()) {
                        getParentFragmentManager().setFragmentResult(rk, bun);
                        // and a decorator from ThreeDs.dialog() will dismiss us
                    } else {
                        requireArguments().putBundle("result", bun);
                    }
                }
            }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        Bundle args = requireArguments();
        Bundle result = args.getBundle("result");
        if (result != null) {
            getParentFragmentManager().setFragmentResult(args.getString("rk"), result);
            dismiss();
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
