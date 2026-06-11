package com.example.ufabcirco.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ufabcirco.R;

public class HorariosFragment extends Fragment {

    private static final String TARGET_URL = "https://ufabcirco.my.canva.site/oficinas";
    private static final String ALLOWED_DOMAIN = "ufabcirco.my.canva.site";

    public static HorariosFragment newInstance() {
        return new HorariosFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_horarios, container, false);

        WebView webView = view.findViewById(R.id.webview_horarios);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.contains(ALLOWED_DOMAIN)) {
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(TARGET_URL);

        return view;
    }
}