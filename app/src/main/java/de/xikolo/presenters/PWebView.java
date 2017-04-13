package de.xikolo.presenters;

import android.net.Uri;

import java.util.Map;

public interface PWebView extends LoadingStateView {

    void showInvalidUrlToast();

    void showErrorToast(String message);

    boolean inAppLinksEnabled();

    boolean externalLinksEnabled();

    void loadUrl(String url, Map<String, String> header);

    void showWebView();

    void hideWebView();

    boolean webViewIsShown();

    void openUrlInBrowser(Uri uri, String token);

}