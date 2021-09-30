package ru.cloudpayments.sdk.three_ds;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ThreeDsDialogFragment extends DialogFragment implements DialogInterface {

    protected ThreeDSDialogListener listener;
    protected WebView webView;

    @Deprecated public ThreeDsDialogFragment() {
    }

    public ThreeDsDialogFragment(String acsUrl, String md, String paReq, String termUrl) {
        Bundle args = new Bundle(2);
        args.putStringArray("args", new String[]{ acsUrl, md, paReq, termUrl });
        setArguments(args);
    }

    public static ThreeDsDialogFragment newInstance(String acsUrl, String md, String paReq) {
        return new ThreeDsDialogFragment(acsUrl, md, paReq, "https://demo.cloudpayments.ru/WebFormPost/GetWebViewData");
    }

    public ThreeDsDialogFragment fragmentResult(String requestKey) {
        requireArguments().putString("rk", requestKey);
        return this;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String[] args = requireArguments().getStringArray("args");
        String acsUrl = args[0];
        String md = args[1];
        String paReq = args[2];
        String termUrl = args[3];

        Activity activity = requireActivity();
        (webView = ThreeDs.view(activity)).setWebViewClient(new ThreeDs.WebViewClient(
            activity, new ThreeDs.DialogFragmentResultListener(this, this, listener), md, termUrl
        ));

        if (savedInstanceState == null)
            webView.postUrl(acsUrl, ThreeDs.requestBody(paReq, md, termUrl));
        else
            webView.restoreState(savedInstanceState);

        return webView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        if (ThreeDs.DialogFragmentResultListener.tryDeliver(this)) {
            dismiss();
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

    // In DialogInterface, we mind only dismiss() (which is already implemented).
    @Override public void cancel() {}

}
