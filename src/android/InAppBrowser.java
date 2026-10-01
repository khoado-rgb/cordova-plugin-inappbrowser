/**
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package org.apache.cordova.inappbrowser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.provider.Browser;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

@SuppressLint("SetJavaScriptEnabled")
public class InAppBrowser extends CordovaPlugin {

    private static final String NULL = "null";
    protected static final String LOG_TAG = "InAppBrowser";
    private static final String SELF = "_self";
    private static final String SYSTEM = "_system";
    private static final String EXIT_EVENT = "exit";
    private static final String LOCATION = "location";
    private static final String ZOOM = "zoom";
    private static final String ZOOMCONTROLS = "zoomcontrols";
    private static final String HIDDEN = "hidden";
    private static final String LOAD_START_EVENT = "loadstart";
    private static final String LOAD_STOP_EVENT = "loadstop";
    private static final String LOAD_ERROR_EVENT = "loaderror";
    private static final String DOWNLOAD_EVENT = "download";
    private static final String MESSAGE_EVENT = "message";
    private static final String CLEAR_ALL_CACHE = "clearcache";
    private static final String CLEAR_SESSION_CACHE = "clearsessioncache";
    private static final String HARDWARE_BACK_BUTTON = "hardwareback";
    private static final String MEDIA_PLAYBACK_REQUIRES_USER_ACTION = "mediaPlaybackRequiresUserAction";
    private static final String SHOULD_PAUSE = "shouldPauseOnSuspend";
    private static final Boolean DEFAULT_HARDWARE_BACK = true;
    private static final String USER_WIDE_VIEW_PORT = "useWideViewPort";
    private static final String TOOLBAR_COLOR = "toolbarcolor";
    private static final String CLOSE_BUTTON_CAPTION = "closebuttoncaption";
    private static final String CLOSE_BUTTON_COLOR = "closebuttoncolor";
    private static final String LEFT_TO_RIGHT = "lefttoright";
    private static final String HIDE_NAVIGATION = "hidenavigationbuttons";
    private static final String NAVIGATION_COLOR = "navigationbuttoncolor";
    private static final String HIDE_URL = "hideurlbar";
    private static final String FOOTER = "footer";
    private static final String FOOTER_COLOR = "footercolor";
    private static final String BEFORELOAD = "beforeload";
    private static final String FULLSCREEN = "fullscreen";
    // OutSystems fork: status bar color (#RRGGBB) and icon style (lightcontent, darkcontent)
    private static final String STATUS_BAR_COLOR = "statusbarcolor";
    private static final String STATUS_BAR_STYLE = "statusbarstyle";
    // OutSystems fork: close button as an X icon; closebuttoncaption then only labels it for TalkBack
    private static final String CLOSE_BUTTON_ICON = "closebuttonicon";
    // OutSystems fork: toolbar height in dp; default TOOLBAR_HEIGHT
    private static final String TOOLBAR_HEIGHT_OPTION = "toolbarheight";
    // OutSystems fork: origins whose pages may ask for the camera, microphone and location,
    // separated by "|". Without it, any origin may ask. The user always confirms first.
    private static final String PERMISSION_ORIGINS = "permissionorigins";
    // OutSystems fork: title on the left of the toolbar, in the place of the hidden URL bar
    private static final String TOOLBAR_TITLE = "toolbartitle";
    // OutSystems fork: size of the close button X (closebuttonicon) or of its caption, in dp/sp
    private static final String CLOSE_BUTTON_SIZE = "closebuttonsize";
    // OutSystems fork: the page itself may only load over https; http navigations of the main frame are blocked
    private static final String HTTPS_ONLY = "httpsonly";

    private static final int TOOLBAR_HEIGHT = 48;
    private static final float MIN_CLOSE_BUTTON_SIZE = 8;
    private static final float MAX_CLOSE_BUTTON_SIZE = 40;
    private static final int MAX_POPUP_WEBVIEWS = 3;
    // httpsonly: the same http page (query and fragment aside) blocked again within this time, or this
    // many http pages blocked within INSECURE_BURST_MS, show a blank page rather than the page before
    private static final long INSECURE_REPEAT_MS = 10000;
    private static final int INSECURE_BURST_COUNT = 3;
    private static final long INSECURE_BURST_MS = 60000;

    private static final List customizableOptions = Arrays.asList(CLOSE_BUTTON_CAPTION, TOOLBAR_COLOR, NAVIGATION_COLOR, CLOSE_BUTTON_COLOR, FOOTER_COLOR, STATUS_BAR_COLOR, STATUS_BAR_STYLE, TOOLBAR_HEIGHT_OPTION, PERMISSION_ORIGINS, TOOLBAR_TITLE, CLOSE_BUTTON_SIZE);

    private InAppBrowserDialog dialog;
    private WebView inAppWebView;
    private EditText edittext;
    private CallbackContext callbackContext;
    private boolean showLocationBar = true;
    private boolean enableZoom = true;
    private boolean showZoomControls = true;
    private boolean openWindowHidden = false;
    private boolean clearAllCache = false;
    private boolean clearSessionCache = false;
    private boolean hadwareBackButton = true;
    private boolean mediaPlaybackRequiresUserGesture = false;
    private boolean shouldPauseInAppBrowser = false;
    private boolean useWideViewPort = true;
    private ValueCallback<Uri[]> mUploadCallback;
    private final static int FILECHOOSER_REQUESTCODE = 1;
    private String closeButtonCaption = "";
    private String closeButtonColor = "";
    private boolean leftToRight = false;
    private int toolbarColor = android.graphics.Color.LTGRAY;
    private boolean hideNavigationButtons = false;
    private String navigationButtonColor = "";
    private boolean hideUrlBar = false;
    private boolean showFooter = false;
    private String footerColor = "";
    private String beforeload = "";
    private boolean fullscreen = true;
    private Integer statusBarColor = null;
    private String statusBarStyle = "";
    private boolean closeButtonIcon = false;
    private int toolbarHeight = TOOLBAR_HEIGHT;
    private String toolbarTitle = "";
    private float closeButtonSize = 0;
    private String[] permissionOrigins = null;
    // "<origin> <resource>" pairs the user allowed while this browser is open
    private final Set<String> allowedPermissions = new HashSet<String>();
    private final Map<Integer, PendingPermissions> pendingPermissions = new HashMap<Integer, PendingPermissions>();
    private int nextPermissionRequestCode = 0;
    private AlertDialog permissionDialog;
    // Transport WebViews created for window.open, destroyed once they are no longer needed
    private final List<WebView> popupWebViews = new ArrayList<WebView>();
    private final Set<WebView> destroyedWebViews = Collections.newSetFromMap(new WeakHashMap<WebView, Boolean>());
    private boolean httpsOnly = false;
    private String[] allowedSchemes;
    private InAppBrowserClient currentClient;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action the action to execute.
     * @param args JSONArry of arguments for the plugin.
     * @param callbackContext the callbackContext used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            final String url = args.getString(0);
            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final HashMap<String, String> features = parseFeature(args.optString(2));

            LOG.d(LOG_TAG, "target = " + target);

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        LOG.d(LOG_TAG, "in self");
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method iuw = Config.class.getMethod("isUrlWhiteListed", String.class);
                                shouldAllowNavigation = (Boolean)iuw.invoke(null, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method gpm = webView.getClass().getMethod("getPluginManager");
                                PluginManager pm = (PluginManager)gpm.invoke(webView);
                                Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean)san.invoke(pm, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            LOG.d(LOG_TAG, "loading in webview");
                            webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                LOG.d(LOG_TAG, "loading in dialer");
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                cordova.getActivity().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                            }
                        }
                        // load in InAppBrowser
                        else {
                            LOG.d(LOG_TAG, "loading in InAppBrowser");
                            InAppBrowser.this.callbackContext = callbackContext;
                            result = showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        LOG.d(LOG_TAG, "in system");
                        result = openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        LOG.d(LOG_TAG, "in blank");
                        InAppBrowser.this.callbackContext = callbackContext;
                        result = showWebPage(url, features);
                    }

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            closeDialog();
        }
        else if (action.equals("loadAfterBeforeload")) {
            if (beforeload == null) {
                LOG.e(LOG_TAG, "unexpected loadAfterBeforeload called without feature beforeload=yes");
            }
            final String url = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    if (inAppWebView == null) {
                        // Closed in the meantime
                        return;
                    }
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        currentClient.waitForBeforeload = false;
                        inAppWebView.setWebViewClient(currentClient);
                    } else {
                        ((InAppBrowserClient)inAppWebView.getWebViewClient()).waitForBeforeload = false;
                    }
                    inAppWebView.loadUrl(url);

                }
            });
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(){prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')})()", callbackContext.getCallbackId());
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.show();
                    }
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else if (action.equals("hide")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.hide();
                    }
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        closeDialog();
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {
        if (shouldPauseInAppBrowser && inAppWebView != null) {
            inAppWebView.onPause();
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        if (shouldPauseInAppBrowser && inAppWebView != null) {
            inAppWebView.onResume();
        }
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        closeDialog();
    }

    /**
     * Inject an object (script or style) into the InAppBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(String source, String jsWrapper) {
        if (inAppWebView!=null) {
            String scriptToInject;
            if (jsWrapper != null) {
                org.json.JSONArray jsonEsc = new org.json.JSONArray();
                jsonEsc.put(source);
                String jsonRepr = jsonEsc.toString();
                String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
                scriptToInject = String.format(jsWrapper, jsonSourceString);
            } else {
                scriptToInject = source;
            }
            final String finalScriptToInject = scriptToInject;
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    inAppWebView.evaluateJavascript(finalScriptToInject, null);
                }
            });
        } else {
            LOG.d(LOG_TAG, "Can't inject code into the system browser");
        }
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private HashMap<String, String> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, String> map = new HashMap<String, String>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while(features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    String value = option.nextToken();
                    if (!customizableOptions.contains(key)) {
                        value = value.equals("yes") || value.equals("no") ? value : "yes";
                    }
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @return "" if ok, or error message.
     */
    public String openExternal(String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
            // CB-10795: Avoid circular loops by preventing it from opening in the current app
            this.openExternalExcludeCurrentApp(intent);
            return "";
            // not catching FileUriExposedException explicitly because buildtools<24 doesn't know about it
        } catch (java.lang.RuntimeException e) {
            LOG.d(LOG_TAG, "InAppBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Opens the intent, providing a chooser that excludes the current app to avoid
     * circular loops.
     */
    private void openExternalExcludeCurrentApp(Intent intent) {
        String currentPackage = cordova.getActivity().getPackageName();
        boolean hasCurrentPackage = false;

        PackageManager pm = cordova.getActivity().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<Intent>();

        for (ResolveInfo ri : activities) {
            if (!currentPackage.equals(ri.activityInfo.packageName)) {
                Intent targetIntent = (Intent)intent.clone();
                targetIntent.setPackage(ri.activityInfo.packageName);
                targetIntents.add(targetIntent);
            }
            else {
                hasCurrentPackage = true;
            }
        }

        // If the current app package isn't a target for this URL, then use
        // the normal launch behavior
        if (hasCurrentPackage == false || targetIntents.size() == 0) {
            this.cordova.getActivity().startActivity(intent);
        }
        // If there's only one possible intent, launch it directly
        else if (targetIntents.size() == 1) {
            this.cordova.getActivity().startActivity(targetIntents.get(0));
        }
        // Otherwise, show a custom chooser without the current app listed
        else if (targetIntents.size() > 0) {
            Intent chooser = Intent.createChooser(targetIntents.remove(targetIntents.size()-1), null);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[] {}));
            this.cordova.getActivity().startActivity(chooser);
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WebView childView = inAppWebView;
                // The JS protects against multiple calls, so this should happen only when
                // closeDialog() is called by other native code.
                if (childView == null) {
                    return;
                }

                // OutSystems fork: nothing the page asked for is granted once it is closed.
                dismissPermissionDialog();
                pendingPermissions.clear();
                destroyPopupWebViews();

                childView.setWebViewClient(new WebViewClient() {
                    private boolean closed = false;

                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (closed) {
                            return;
                        }
                        closed = true;
                        // OutSystems fork: if a new browser was opened meanwhile, its dialog stays.
                        if (dialog != null && inAppWebView == childView && !cordova.getActivity().isFinishing()) {
                            dialog.dismiss();
                            dialog = null;
                        }
                        // OutSystems fork: free the native WebView now rather than at the next GC.
                        destroyWebView(childView);
                    }
                });
                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", EXIT_EVENT);
                    sendUpdate(obj, false);
                } catch (JSONException ex) {
                    LOG.d(LOG_TAG, "Should never happen");
                }
            }
        });
    }

    /**
     * OutSystems fork: releases a WebView right away instead of at the next GC. Posted, since it
     * may be called from one of the WebView's own callbacks.
     */
    private void destroyWebView(final WebView webView) {
        if (webView == null || !destroyedWebViews.add(webView)) {
            return;
        }
        if (inAppWebView == webView) {
            inAppWebView = null;
            currentClient = null;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ViewParent parent = webView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(webView);
                }
                webView.stopLoading();
                webView.destroy();
            }
        });
    }

    private void destroyPopupWebView(WebView popup) {
        if (popupWebViews.remove(popup)) {
            destroyWebView(popup);
        }
    }

    private void destroyPopupWebViews() {
        for (WebView popup : new ArrayList<WebView>(popupWebViews)) {
            destroyPopupWebView(popup);
        }
    }

    /**
     * OutSystems fork: camera and microphone requests from a page. Only pages on a
     * permissionorigins origin may ask. The user confirms in a dialog, once per origin while the
     * browser is open, then Android asks for its runtime permission if the app does not have it yet.
     */
    private void onPagePermissionRequest(final PermissionRequest request) {
        final String origin = originOf(request.getOrigin());
        final List<String> resources = new ArrayList<String>();
        final List<String> androidPermissions = new ArrayList<String>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                resources.add(resource);
                androidPermissions.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                resources.add(resource);
                androidPermissions.add(Manifest.permission.RECORD_AUDIO);
            }
            // Other resources (protected media, MIDI) are never granted.
        }
        if (resources.isEmpty() || !isPermissionOrigin(origin)) {
            answerPermissionRequest(request, null);
            return;
        }

        boolean camera = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        boolean microphone = resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        String message = camera && microphone
                ? localized("muốn dùng camera và micro của bạn.", "wants to use your camera and microphone.")
                : camera
                        ? localized("muốn dùng camera của bạn.", "wants to use your camera.")
                        : localized("muốn dùng micro của bạn.", "wants to use your microphone.");
        List<String> keys = new ArrayList<String>();
        for (String resource : resources) {
            keys.add(origin + " " + resource);
        }
        askUser(origin, keys, message, new Runnable() {
            @Override
            public void run() {
                requestAndroidPermissions(androidPermissions, new PermissionsCallback() {
                    @Override
                    public void onResult(Set<String> granted) {
                        // Grant what Android allows too, e.g. the camera without the microphone.
                        List<String> allowed = new ArrayList<String>();
                        for (int i = 0; i < resources.size(); i++) {
                            if (granted.contains(androidPermissions.get(i))) {
                                allowed.add(resources.get(i));
                            }
                        }
                        answerPermissionRequest(request, allowed);
                    }
                });
            }
        }, new Runnable() {
            @Override
            public void run() {
                answerPermissionRequest(request, null);
            }
        });
    }

    // grant() and deny() throw once the request was answered or cancelled by the page.
    private void answerPermissionRequest(PermissionRequest request, List<String> resources) {
        try {
            if (resources == null || resources.isEmpty()) {
                request.deny();
            } else {
                request.grant(resources.toArray(new String[0]));
            }
        } catch (RuntimeException e) {
            LOG.d(LOG_TAG, "Permission request no longer pending: " + e.getMessage());
        }
    }

    /**
     * OutSystems fork: location requests from a page, same rules as onPagePermissionRequest.
     * WebView does not retain the answer; askUser remembers it until the browser closes.
     */
    private void onPageGeolocationRequest(final String origin, final GeolocationPermissions.Callback callback) {
        final String pageOrigin = originOf(Uri.parse(origin));
        if (!isPermissionOrigin(pageOrigin)) {
            callback.invoke(origin, false, false);
            return;
        }
        final List<String> androidPermissions = Arrays.asList(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        askUser(pageOrigin, Arrays.asList(pageOrigin + " location"),
                localized("muốn biết vị trí của bạn.", "wants to know your location."), new Runnable() {
            @Override
            public void run() {
                requestAndroidPermissions(androidPermissions, new PermissionsCallback() {
                    @Override
                    public void onResult(Set<String> granted) {
                        // Approximate location alone is enough.
                        callback.invoke(origin, !granted.isEmpty(), false);
                    }
                });
            }
        }, new Runnable() {
            @Override
            public void run() {
                callback.invoke(origin, false, false);
            }
        });
    }

    // Asks the user unless all keys ("<origin> <resource>") were allowed while the browser is open.
    // One question at a time.
    private void askUser(String origin, final List<String> keys, String message, final Runnable onAllow, final Runnable onDeny) {
        if (allowedPermissions.containsAll(keys)) {
            onAllow.run();
            return;
        }
        if (permissionDialog != null) {
            onDeny.run();
            return;
        }
        final boolean[] answered = { false };
        final AlertDialog question = new AlertDialog.Builder(cordova.getActivity())
                .setMessage(Uri.parse(origin).getHost() + " " + message)
                .setPositiveButton(localized("Cho phép", "Allow"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        answered[0] = true;
                        allowedPermissions.addAll(keys);
                        onAllow.run();
                    }
                })
                .setNegativeButton(localized("Không cho phép", "Don't allow"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        answered[0] = true;
                        onDeny.run();
                    }
                })
                .create();
        // Back, a tap outside, the page cancelling or the browser closing all count as a refusal.
        question.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                if (permissionDialog == question) {
                    permissionDialog = null;
                }
                if (!answered[0]) {
                    answered[0] = true;
                    onDeny.run();
                }
            }
        });
        permissionDialog = question;
        try {
            question.show();
        } catch (RuntimeException e) {
            // The activity is going away.
            permissionDialog = null;
            answered[0] = true;
            onDeny.run();
        }
    }

    private void dismissPermissionDialog() {
        AlertDialog question = permissionDialog;
        permissionDialog = null;
        if (question != null) {
            question.dismiss();
        }
    }

    private boolean isPermissionOrigin(String origin) {
        return !origin.isEmpty() && (permissionOrigins == null || Arrays.asList(permissionOrigins).contains(origin));
    }

    // origin and path of a URL, without its query and fragment; "/" for an empty path
    private static String pageOf(String url) {
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        return originOf(uri) + (path == null || path.isEmpty() ? "/" : path);
    }

    // scheme://host[:port] without the default port, like window.location.origin
    private static String originOf(Uri uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            return "";
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
    }

    private static String localized(String vietnamese, String english) {
        return "vi".equals(Locale.getDefault().getLanguage()) ? vietnamese : english;
    }

    private interface PermissionsCallback {
        void onResult(Set<String> granted);
    }

    private static class PendingPermissions {
        final Set<String> granted;
        final PermissionsCallback callback;

        PendingPermissions(Set<String> granted, PermissionsCallback callback) {
            this.granted = granted;
            this.callback = callback;
        }
    }

    // Asks Android for the runtime permissions the app does not have yet.
    private void requestAndroidPermissions(List<String> permissions, PermissionsCallback callback) {
        Set<String> granted = new HashSet<String>();
        List<String> missing = new ArrayList<String>();
        for (String permission : permissions) {
            if (cordova.hasPermission(permission)) {
                granted.add(permission);
            } else if (!missing.contains(permission)) {
                missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            callback.onResult(granted);
            return;
        }
        int requestCode = nextPermissionRequestCode++;
        pendingPermissions.put(requestCode, new PendingPermissions(granted, callback));
        cordova.requestPermissions(this, requestCode, missing.toArray(new String[0]));
    }

    // Called by cordova-android up to 14.
    @Override
    @SuppressWarnings("deprecation")
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        onPermissionsResult(requestCode, permissions, grantResults);
    }

    // Newer name of the callback; no @Override, since older cordova-android versions lack it.
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        onPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PendingPermissions pending = pendingPermissions.remove(requestCode);
        if (pending == null) {
            return;
        }
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                pending.granted.add(permissions[i]);
            }
        }
        pending.callback.onResult(pending.granted);
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView != null && this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView != null && this.inAppWebView.canGoBack();
    }

    /**
     * Has the user set the hardware back button to go back
     * @return boolean
     */
    public boolean hardwareBack() {
        return hadwareBackButton;
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView != null && this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(String url) {
        if (this.inAppWebView == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }


    /**
     * Should we show the location bar?
     *
     * @return boolean
     */
    private boolean getShowLocationBar() {
        return this.showLocationBar;
    }

    private InAppBrowser getInAppBrowser() {
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @param features jsonObject
     */
    public String showWebPage(final String url, HashMap<String, String> features) {
        // Determine if we should hide the location bar.
        showLocationBar = true;
        enableZoom = true;
        showZoomControls = true;
        openWindowHidden = false;
        mediaPlaybackRequiresUserGesture = false;
        statusBarColor = null;
        statusBarStyle = "";
        closeButtonIcon = false;
        toolbarHeight = TOOLBAR_HEIGHT;
        toolbarTitle = "";
        closeButtonSize = 0;
        permissionOrigins = null;
        httpsOnly = false;
        allowedPermissions.clear();
        pendingPermissions.clear();
        dismissPermissionDialog();

        if (features != null) {
            String show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.equals("yes") ? true : false;
            }
            if(showLocationBar) {
                String hideNavigation = features.get(HIDE_NAVIGATION);
                String hideUrl = features.get(HIDE_URL);
                if(hideNavigation != null) hideNavigationButtons = hideNavigation.equals("yes") ? true : false;
                if(hideUrl != null) hideUrlBar = hideUrl.equals("yes") ? true : false;
            }
            String zoom = features.get(ZOOM);
            if (zoom != null) {
                enableZoom = zoom.equals("yes");
            }
            String zoomcontrols = features.get(ZOOMCONTROLS);
            if (zoomcontrols != null) {
                showZoomControls = zoomcontrols.equals("yes");
            }
            String hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.equals("yes") ? true : false;
            }
            String hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                hadwareBackButton = hardwareBack.equals("yes") ? true : false;
            } else {
                hadwareBackButton = DEFAULT_HARDWARE_BACK;
            }
            String mediaPlayback = features.get(MEDIA_PLAYBACK_REQUIRES_USER_ACTION);
            if (mediaPlayback != null) {
                mediaPlaybackRequiresUserGesture = mediaPlayback.equals("yes") ? true : false;
            }
            String cache = features.get(CLEAR_ALL_CACHE);
            if (cache != null) {
                clearAllCache = cache.equals("yes") ? true : false;
            } else {
                cache = features.get(CLEAR_SESSION_CACHE);
                if (cache != null) {
                    clearSessionCache = cache.equals("yes") ? true : false;
                }
            }
            String shouldPause = features.get(SHOULD_PAUSE);
            if (shouldPause != null) {
                shouldPauseInAppBrowser = shouldPause.equals("yes") ? true : false;
            }
            String wideViewPort = features.get(USER_WIDE_VIEW_PORT);
            if (wideViewPort != null ) {
                useWideViewPort = wideViewPort.equals("yes") ? true : false;
            }
            String closeButtonCaptionSet = features.get(CLOSE_BUTTON_CAPTION);
            if (closeButtonCaptionSet != null) {
                closeButtonCaption = closeButtonCaptionSet;
            }
            String closeButtonColorSet = features.get(CLOSE_BUTTON_COLOR);
            if (closeButtonColorSet != null) {
                closeButtonColor = closeButtonColorSet;
            }
            String leftToRightSet = features.get(LEFT_TO_RIGHT);
            leftToRight = leftToRightSet != null && leftToRightSet.equals("yes");

            String toolbarColorSet = features.get(TOOLBAR_COLOR);
            if (toolbarColorSet != null) {
                toolbarColor = android.graphics.Color.parseColor(toolbarColorSet);
            }
            String navigationButtonColorSet = features.get(NAVIGATION_COLOR);
            if (navigationButtonColorSet != null) {
                navigationButtonColor = navigationButtonColorSet;
            }
            String showFooterSet = features.get(FOOTER);
            if (showFooterSet != null) {
                showFooter = showFooterSet.equals("yes") ? true : false;
            }
            String footerColorSet = features.get(FOOTER_COLOR);
            if (footerColorSet != null) {
                footerColor = footerColorSet;
            }
            if (features.get(BEFORELOAD) != null) {
                beforeload = features.get(BEFORELOAD);
            }
            String fullscreenSet = features.get(FULLSCREEN);
            if (fullscreenSet != null) {
                fullscreen = fullscreenSet.equals("yes") ? true : false;
            }
            String statusBarColorSet = features.get(STATUS_BAR_COLOR);
            if (statusBarColorSet != null) {
                try {
                    statusBarColor = Color.parseColor(statusBarColorSet);
                } catch (IllegalArgumentException e) {
                    LOG.e(LOG_TAG, "Invalid statusbarcolor: " + statusBarColorSet);
                }
            }
            String statusBarStyleSet = features.get(STATUS_BAR_STYLE);
            if (statusBarStyleSet != null) {
                statusBarStyle = statusBarStyleSet;
            }
            closeButtonIcon = "yes".equals(features.get(CLOSE_BUTTON_ICON));
            String toolbarHeightSet = features.get(TOOLBAR_HEIGHT_OPTION);
            if (toolbarHeightSet != null) {
                try {
                    int height = Math.round(Float.parseFloat(toolbarHeightSet));
                    if (height > 0) {
                        toolbarHeight = height;
                    }
                } catch (NumberFormatException e) {
                    LOG.e(LOG_TAG, "Invalid toolbarheight: " + toolbarHeightSet);
                }
            }
            String closeButtonSizeSet = features.get(CLOSE_BUTTON_SIZE);
            if (closeButtonSizeSet != null) {
                try {
                    float size = Float.parseFloat(closeButtonSizeSet);
                    // 0 or less keeps the default; NaN and infinity are dropped.
                    if (size > 0 && !Float.isInfinite(size)) {
                        closeButtonSize = Math.min(Math.max(size, MIN_CLOSE_BUTTON_SIZE), MAX_CLOSE_BUTTON_SIZE);
                    }
                } catch (NumberFormatException e) {
                    LOG.e(LOG_TAG, "Invalid closebuttonsize: " + closeButtonSizeSet);
                }
            }
            String toolbarTitleSet = features.get(TOOLBAR_TITLE);
            if (toolbarTitleSet != null) {
                toolbarTitle = toolbarTitleSet.trim();
            }
            String permissionOriginsSet = features.get(PERMISSION_ORIGINS);
            if (permissionOriginsSet != null) {
                // Invalid and non-https entries are dropped, so a list with none left denies every origin.
                List<String> origins = new ArrayList<String>();
                for (String entry : permissionOriginsSet.split("\\|")) {
                    String origin = originOf(Uri.parse(entry.trim()));
                    if (origin.startsWith("https://")) {
                        origins.add(origin);
                    }
                }
                permissionOrigins = origins.toArray(new String[0]);
            }
            httpsOnly = "yes".equals(features.get(HTTPS_ONLY));
        }

        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(int dipValue) {
                int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                        (float) dipValue,
                        cordova.getActivity().getResources().getDisplayMetrics()
                );

                return value;
            }

            /**
             * Clears cookies according to the active options and invokes completion when finished.
             *
             * Cookie clearing APIs are asynchronous (API 22+), so loadUrl must run in the callback
             * to avoid a race where navigation starts before cookie deletion is complete.
             */
            private void clearCookies(final Runnable completion) {
                final CookieManager cookieManager = CookieManager.getInstance();

                if (clearAllCache) {
                    cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean value) {
                            cookieManager.flush();
                            completion.run();
                        }
                    });
                } else if (clearSessionCache) {
                    cookieManager.removeSessionCookies(new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean value) {
                            cookieManager.flush();
                            completion.run();
                        }
                    });
                } else {
                    completion.run();
                }
            }

            private View createCloseButton(int id) {
                View _close;
                Resources activityRes = cordova.getActivity().getResources();

                if (closeButtonCaption != "" && !closeButtonIcon) {
                    // Use TextView for text
                    TextView close = new TextView(cordova.getActivity());
                    close.setText(closeButtonCaption);
                    close.setTextSize(closeButtonSize > 0 ? closeButtonSize : 20);
                    if (closeButtonColor != "") close.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                    close.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    // OutSystems fork: the caption ends 16dp from the edge, with the toolbar's 2dp padding,
                    // like the title on the other side.
                    int edgePadding = this.dpToPixels(14);
                    int innerPadding = this.dpToPixels(10);
                    close.setPadding(leftToRight ? edgePadding : innerPadding, 0, leftToRight ? innerPadding : edgePadding, 0);
                    _close = close;
                }
                else {
                    ImageButton close = new ImageButton(cordova.getActivity());
                    // OutSystems fork: closebuttonicon uses an opaque 18dp X, the same as on iOS.
                    int closeResId = closeButtonIcon
                            ? activityRes.getIdentifier("ic_miniapp_close", "drawable", cordova.getActivity().getPackageName())
                            : 0;
                    if (closeResId == 0) {
                        closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", cordova.getActivity().getPackageName());
                    }
                    Drawable closeIcon = activityRes.getDrawable(closeResId);
                    if (closeButtonColor != "") close.setColorFilter(android.graphics.Color.parseColor(closeButtonColor));
                    close.setImageDrawable(closeIcon);
                    // FIT_CENTER also scales the 18dp X to closebuttonsize, as wide as the view leaves it.
                    close.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    close.getAdjustViewBounds();

                    _close = close;
                }

                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                if (leftToRight) closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                else closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                _close.setLayoutParams(closeLayoutParams);
                _close.setBackground(null);
                if (closeButtonIcon) {
                    // OutSystems fork: the X (18dp, or closebuttonsize) ends 16dp from the edge, like the
                    // title on the other side: 2dp toolbar padding, a margin, then padding on each side of
                    // the glyph for a touch area of at least 44dp, as far as the 14dp left to the edge allows.
                    DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
                    float glyph = closeButtonSize > 0 ? closeButtonSize : 18;
                    float sidePadding = Math.min(Math.max(0, (44 - glyph) / 2), 14);
                    int sidePaddingPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sidePadding, metrics));
                    _close.setPadding(sidePaddingPx, 0, sidePaddingPx, 0);
                    closeLayoutParams.width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, glyph + 2 * sidePadding, metrics));
                    int edgeMargin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14 - sidePadding, metrics));
                    if (leftToRight) {
                        closeLayoutParams.leftMargin = edgeMargin;
                    } else {
                        closeLayoutParams.rightMargin = edgeMargin;
                    }
                }

                // With closebuttonicon, the caption names the icon for TalkBack
                _close.setContentDescription(closeButtonCaption != "" ? closeButtonCaption : "Close Button");
                _close.setId(Integer.valueOf(id));
                _close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });

                return _close;
            }

            /**
             * OutSystems fork: colors the status bar of the dialog window and sets its icon style.
             *
             * From Android 11 the dialog is drawn edge-to-edge (see run()): the window's own bars are
             * transparent and the status bar spacer shows through, as setStatusBarColor has no effect
             * for targetSdk 35 on Android 15+. Up to Android 10 the window draws the status bar color.
             */
            @SuppressLint("NewApi")
            private void styleStatusBar(Window window) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.TRANSPARENT);
                    window.setNavigationBarColor(Color.TRANSPARENT);
                } else if (statusBarColor != null) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(statusBarColor);
                }
                if (statusBarStyle.isEmpty()) {
                    return;
                }
                // Same values as iOS: darkcontent = dark icons, for a light status bar color.
                boolean darkIcons = statusBarStyle.equals("darkcontent");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Also set from Android 11: there the insets controller alone does not always
                    // take effect (AndroidX WindowInsetsControllerCompat sets both as well).
                    View decor = window.getDecorView();
                    int flags = decor.getSystemUiVisibility();
                    decor.setSystemUiVisibility(darkIcons
                            ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = window.getInsetsController();
                    if (controller != null) {
                        controller.setSystemBarsAppearance(
                                darkIcons ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                }
            }

            @SuppressLint("NewApi")
            public void run() {

                // CB-6702 InAppBrowser hangs when opening more than one instance
                if (dialog != null) {
                    dialog.dismiss();
                };
                // OutSystems fork: also free the previous WebViews, in case the last close did not finish.
                destroyWebView(inAppWebView);
                destroyPopupWebViews();

                // Let's create the main dialog
                dialog = new InAppBrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                if (fullscreen) {
                    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                dialog.setCancelable(true);
                dialog.setInAppBroswer(getInAppBrowser());

                // Main container layout
                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                // OutSystems fork: status bar backdrop, sized from the insets once the dialog is drawn
                // edge-to-edge (Android 11+). Up to Android 10 it stays 0 high.
                final View statusBarSpacer = new View(cordova.getActivity());
                statusBarSpacer.setBackgroundColor(statusBarColor != null ? statusBarColor
                        : (getShowLocationBar() ? toolbarColor : Color.BLACK));
                statusBarSpacer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0));
                main.addView(statusBarSpacer);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // The dialog is drawn edge-to-edge, also over the display cutout, so that the status
                    // bar spacer can paint the area behind the status bar. The window then neither fits
                    // the system bars nor resizes for the keyboard: keep the toolbar below the status bar,
                    // and the web view above the navigation bar and the keyboard.
                    dialog.getWindow().setDecorFitsSystemWindows(false);
                    WindowManager.LayoutParams windowAttributes = dialog.getWindow().getAttributes();
                    windowAttributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    dialog.getWindow().setAttributes(windowAttributes);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    // Shows behind the navigation bar, like the legacy black navigation bar.
                    main.setBackgroundColor(Color.BLACK);
                    main.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                            android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                            ViewGroup.LayoutParams spacerParams = statusBarSpacer.getLayoutParams();
                            if (spacerParams.height != bars.top) {
                                spacerParams.height = bars.top;
                                statusBarSpacer.setLayoutParams(spacerParams);
                            }
                            v.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
                            // Consumed, so the web view does not report them again as CSS safe-area insets.
                            return WindowInsets.CONSUMED;
                        }
                    });
                }

                // Toolbar layout
                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black!
                toolbar.setBackgroundColor(toolbarColor);
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(toolbarHeight)));
                toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
                if (leftToRight) {
                    toolbar.setHorizontalGravity(Gravity.LEFT);
                } else {
                    toolbar.setHorizontalGravity(Gravity.RIGHT);
                }
                toolbar.setVerticalGravity(Gravity.TOP);

                // Action Button Container layout
                RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());
                RelativeLayout.LayoutParams actionButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                if (leftToRight) actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                else actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                actionButtonContainer.setLayoutParams(actionButtonLayoutParams);
                actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
                actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                actionButtonContainer.setId(leftToRight ? Integer.valueOf(5) : Integer.valueOf(1));

                // Back button
                ImageButton back = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                back.setLayoutParams(backLayoutParams);
                back.setContentDescription("Back Button");
                back.setId(Integer.valueOf(2));
                Resources activityRes = cordova.getActivity().getResources();
                int backResId = activityRes.getIdentifier("ic_action_previous_item", "drawable", cordova.getActivity().getPackageName());
                Drawable backIcon = activityRes.getDrawable(backResId);
                if (navigationButtonColor != "") back.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
                back.setBackground(null);
                back.setImageDrawable(backIcon);
                back.setScaleType(ImageView.ScaleType.FIT_CENTER);
                back.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
                back.getAdjustViewBounds();

                back.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goBack();
                    }
                });

                // Forward button
                ImageButton forward = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
                forward.setLayoutParams(forwardLayoutParams);
                forward.setContentDescription("Forward Button");
                forward.setId(Integer.valueOf(3));
                int fwdResId = activityRes.getIdentifier("ic_action_next_item", "drawable", cordova.getActivity().getPackageName());
                Drawable fwdIcon = activityRes.getDrawable(fwdResId);
                if (navigationButtonColor != "") forward.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
                forward.setBackground(null);
                forward.setImageDrawable(fwdIcon);
                forward.setScaleType(ImageView.ScaleType.FIT_CENTER);
                forward.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
                forward.getAdjustViewBounds();

                forward.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goForward();
                    }
                });

                // Edit Text Box
                edittext = new EditText(cordova.getActivity());
                RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
                textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                edittext.setLayoutParams(textLayoutParams);
                edittext.setId(Integer.valueOf(4));
                edittext.setSingleLine(true);
                edittext.setText(url);
                edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
                edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
                edittext.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            navigate(edittext.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });


                // Header Close/Done button
                int closeButtonId = leftToRight ? 1 : 5;
                View close = createCloseButton(closeButtonId);
                toolbar.addView(close);

                // OutSystems fork: the title takes the place of the hidden URL bar, between the
                // navigation buttons and the close button (ids 1 and 5, swapped by lefttoright).
                if (!toolbarTitle.isEmpty() && hideUrlBar) {
                    TextView title = new TextView(cordova.getActivity());
                    title.setText(toolbarTitle);
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    if (closeButtonColor != "") title.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                    title.setSingleLine(true);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                    // 16dp from the edge, with the toolbar's own 2dp padding
                    title.setPadding(this.dpToPixels(14), 0, this.dpToPixels(8), 0);
                    RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                    titleLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
                    titleLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                    title.setLayoutParams(titleLayoutParams);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        title.setAccessibilityHeading(true);
                    }
                    toolbar.addView(title);
                }

                // Footer
                RelativeLayout footer = new RelativeLayout(cordova.getActivity());
                int _footerColor;
                if(footerColor != "") {
                    _footerColor = Color.parseColor(footerColor);
                } else {
                    _footerColor = android.graphics.Color.LTGRAY;
                }
                footer.setBackgroundColor(_footerColor);
                RelativeLayout.LayoutParams footerLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(TOOLBAR_HEIGHT));
                footerLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                footer.setLayoutParams(footerLayout);
                if (closeButtonCaption != "") footer.setPadding(this.dpToPixels(8), this.dpToPixels(8), this.dpToPixels(8), this.dpToPixels(8));
                footer.setHorizontalGravity(Gravity.LEFT);
                footer.setVerticalGravity(Gravity.BOTTOM);

                View footerClose = createCloseButton(7);
                footer.addView(footerClose);

                // WebView
                inAppWebView = new WebView(cordova.getActivity());
                inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                inAppWebView.setId(Integer.valueOf(6));
                // File Chooser Implemented ChromeClient
                inAppWebView.setWebChromeClient(new InAppChromeClient(thatWebView) {
                    // OutSystems fork: camera, microphone and location only after the user agrees.
                    @Override
                    public void onPermissionRequest(PermissionRequest request) {
                        onPagePermissionRequest(request);
                    }

                    @Override
                    public void onPermissionRequestCanceled(PermissionRequest request) {
                        dismissPermissionDialog();
                    }

                    @Override
                    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                        onPageGeolocationRequest(origin, callback);
                    }

                    @Override
                    public void onGeolocationPermissionsHidePrompt() {
                        dismissPermissionDialog();
                    }

                    @Override
                    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                        // New-window navigations (for example window.open or target=_blank)
                        // are delivered here by WebView. We do not show a second visible
                        // browser window; instead, we route that navigation back into the
                        // current InAppBrowser WebView so existing lifecycle and beforeload
                        // handling remain consistent.
                        final WebView inAppWebView = view;
                        final WebViewClient webViewClient = new WebViewClient() {
                            /**
                             * New (added in API 24)
                             * For Android 7 and above.
                             */
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                                return handleNewWindowUrl(view, request.getUrl().toString(), request.getMethod());
                            }

                            /**
                             * Legacy (deprecated in API 24)
                             * For Android 6 and below.
                             */
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                return handleNewWindowUrl(view, url, null);
                            }

                            private boolean handleNewWindowUrl(WebView popup, String targetUrl, String method) {
                                // WebView commonly initializes popup flows with about:blank.
                                // Forwarding this placeholder URL into the main WebView can
                                // replace the current page and break subsequent navigation,
                                // so it must be ignored.
                                if ("about:blank".equals(targetUrl)) {
                                    return false;
                                }

                                // OutSystems fork: the navigation leaves the transport WebView,
                                // which is no longer needed.
                                destroyPopupWebView(popup);
                                if (blockInsecure(targetUrl)) {
                                    return true;
                                }

                                // Reuse the main client so beforeload and scheme routing are
                                // applied exactly like regular navigations.
                                if (currentClient != null && currentClient.shouldOverrideUrlLoading(targetUrl, method)) {
                                    return true;
                                }

                                // If the main client did not consume the request, continue by
                                // loading the URL in the currently visible InAppBrowser view.
                                inAppWebView.loadUrl(targetUrl);
                                return true;
                            }
                        };

                        // Attach a temporary transport WebView required by the Android
                        // onCreateWindow contract. Its client forwards navigation decisions
                        // to the active InAppBrowser WebView/client above.
                        final WebView newWebView = new WebView(view.getContext());
                        newWebView.setWebViewClient(webViewClient);
                        // OutSystems fork: tracked so it is destroyed, at the latest when the browser
                        // closes. A page that keeps opening blank windows only keeps the last few.
                        popupWebViews.add(newWebView);
                        while (popupWebViews.size() > MAX_POPUP_WEBVIEWS) {
                            destroyPopupWebView(popupWebViews.get(0));
                        }

                        final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                        transport.setWebView(newWebView);
                        resultMsg.sendToTarget();

                        return true;
                    }

                    public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
                    {
                        LOG.d(LOG_TAG, "File Chooser 5.0+");
                        // If callback exists, finish it.
                        if(mUploadCallback != null) {
                            mUploadCallback.onReceiveValue(null);
                        }
                        mUploadCallback = filePathCallback;

                        // Create File Chooser Intent
                        Intent content = new Intent(Intent.ACTION_GET_CONTENT);
                        content.addCategory(Intent.CATEGORY_OPENABLE);
                        content.setType("*/*");

                        // Run cordova startActivityForResult
                        cordova.startActivityForResult(InAppBrowser.this, Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE);
                        return true;
                    }
                });
                currentClient = new InAppBrowserClient(thatWebView, edittext, beforeload);
                inAppWebView.setWebViewClient(currentClient);
                WebSettings settings = inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                // setBuiltInZoomControls enables pinch-to-zoom and also the on-screen zoom controls
                // The zoom controls have to be disabled separately by setDisplayZoomControls
                // This is the recommended way by Google
                settings.setBuiltInZoomControls(enableZoom);
                // Enable/Disable on-screen zoom controls.
                // These are deprecated since Android API Level 26 (Android 8).
                // Google recommends to disable them
                settings.setDisplayZoomControls(showZoomControls);
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);
                
                // download event
                
                inAppWebView.setDownloadListener(
                    new DownloadListener(){
                        public void onDownloadStart(
                                String url, String userAgent, String contentDisposition, String mimetype, long contentLength
                        ){
                            try{
                                JSONObject succObj = new JSONObject();
                                succObj.put("type", DOWNLOAD_EVENT);
                                succObj.put("url",url);
                                succObj.put("userAgent",userAgent);
                                succObj.put("contentDisposition",contentDisposition);
                                succObj.put("mimetype",mimetype);
                                succObj.put("contentLength",contentLength);
                                sendUpdate(succObj, true);
                            }
                            catch(Exception e){
                                LOG.e(LOG_TAG,e.getMessage());
                            }
                        }
                    }
                );        

                // Add postMessage interface
                class JsObject {
                    @JavascriptInterface
                    public void postMessage(String data) {
                        sendMessageEvent(data, null);
                    }
                }

                settings.setMediaPlaybackRequiresUserGesture(mediaPlaybackRequiresUserGesture);
                // OutSystems fork: with a WebMessageListener only the main frame may talk to the app,
                // not its iframes (ads, analytics), and each message carries the origin of the page
                // that sent it. WebViews without it fall back to the JavascriptInterface.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    WebViewCompat.addWebMessageListener(inAppWebView, "cordova_iab", Collections.singleton("*"),
                        new WebViewCompat.WebMessageListener() {
                            @Override
                            public void onPostMessage(WebView view, WebMessageCompat message, Uri sourceOrigin,
                                                      boolean isMainFrame, JavaScriptReplyProxy replyProxy) {
                                if (!isMainFrame) {
                                    return;
                                }
                                try {
                                    sendMessageEvent(message.getData(), originOf(sourceOrigin));
                                } catch (IllegalStateException e) {
                                    LOG.e(LOG_TAG, "postMessage only takes a JSON string.");
                                }
                            }
                        });
                } else {
                    inAppWebView.addJavascriptInterface(new JsObject(), "cordova_iab");
                }

                String overrideUserAgent = preferences.getString("OverrideUserAgent", null);
                String appendUserAgent = preferences.getString("AppendUserAgent", null);

                if (overrideUserAgent != null) {
                    settings.setUserAgentString(overrideUserAgent);
                }
                if (appendUserAgent != null) {
                    settings.setUserAgentString(settings.getUserAgentString() + " " + appendUserAgent);
                }

                //Toggle whether this is enabled or not!
                Bundle appSettings = cordova.getActivity().getIntent().getExtras();
                boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
                if (enableDatabase) {
                    String databasePath = cordova.getActivity().getApplicationContext().getDir("inAppBrowserDB", Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                // Enable Thirdparty Cookies
                CookieManager.getInstance().setAcceptThirdPartyCookies(inAppWebView,true);

                // Delay navigation until cookie clearing is complete (or skipped).
                clearCookies(new Runnable() {
                    @Override
                    public void run() {
                        inAppWebView.loadUrl(url);
                    }
                });
                inAppWebView.setId(Integer.valueOf(6));
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(useWideViewPort);
                // Multiple Windows set to true to mitigate Chromium security bug.
                //  See: https://bugs.chromium.org/p/chromium/issues/detail?id=1083819
                inAppWebView.getSettings().setSupportMultipleWindows(true);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();

                // Add the back and forward buttons to our action button container layout
                actionButtonContainer.addView(back);
                actionButtonContainer.addView(forward);

                // Add the views to our toolbar if they haven't been disabled
                if (!hideNavigationButtons) toolbar.addView(actionButtonContainer);
                if (!hideUrlBar) toolbar.addView(edittext);

                // Don't add the toolbar if its been disabled
                if (getShowLocationBar()) {
                    // Add our toolbar to our main view/layout
                    main.addView(toolbar);
                }

                // Add our webview to our main view/layout
                RelativeLayout webViewLayout = new RelativeLayout(cordova.getActivity());
                webViewLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
                webViewLayout.addView(inAppWebView);
                main.addView(webViewLayout);

                // Don't add the footer unless it's been enabled
                if (showFooter) {
                    footer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(TOOLBAR_HEIGHT)));
                    main.addView(footer);
                }

                if (dialog != null) {
                    dialog.setContentView(main);
                    styleStatusBar(dialog.getWindow());
                    dialog.show();
                    // OutSystems fork: was setAttributes() with a copy taken before setContentView, which
                    // dropped the flags set since then (system bar backgrounds, the dialog layout flags).
                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                }
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if (openWindowHidden && dialog != null) {
                    dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * OutSystems fork: a postMessage from the page, with the origin of the page that sent it when
     * the WebView reports one.
     */
    private void sendMessageEvent(String data, String origin) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", MESSAGE_EVENT);
            obj.put("data", new JSONObject(data));
            if (origin != null) {
                obj.put("origin", origin);
            }
            sendUpdate(obj, true);
        } catch (JSONException | NullPointerException ex) {
            LOG.e(LOG_TAG, "data object passed to postMessage has caused a JSON error.");
        }
    }

    // OutSystems fork: with httpsonly, an http URL must not load.
    private boolean isInsecure(String url) {
        return httpsOnly && url != null && url.regionMatches(true, 0, "http:", 0, 5);
    }

    /**
     * OutSystems fork: with httpsonly, true for an http URL, which is then reported as a loaderror
     * and must not load.
     */
    private boolean blockInsecure(String url) {
        if (!isInsecure(url)) {
            return false;
        }
        LOG.e(LOG_TAG, "httpsonly: blocked an http page");
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", LOAD_ERROR_EVENT);
            obj.put("url", url);
            obj.put("code", -1);
            obj.put("message", "Only https pages may load (httpsonly)");
            sendUpdate(obj, true, PluginResult.Status.ERROR);
        } catch (JSONException ex) {
            LOG.d(LOG_TAG, "Should never happen");
        }
        return true;
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback) {
        sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
        if (callbackContext != null) {
            PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                callbackContext = null;
            }
        }
    }

    /**
     * Receive File Data from File Chooser
     *
     * @param requestCode the requested code from chromeclient
     * @param resultCode the result code returned from android system
     * @param intent the data from android file chooser
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.d(LOG_TAG, "onActivityResult");
        // If RequestCode or Callback is Invalid
        if(requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        mUploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
        mUploadCallback = null;
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppBrowserClient extends WebViewClient {
        EditText edittext;
        CordovaWebView webView;
        String beforeload;
        boolean waitForBeforeload;
        // OutSystems fork: httpsonly stopped an http page and is showing about:blank instead.
        private boolean blankingInsecurePage = false;
        // OutSystems fork: the last http page httpsonly stopped (without query and fragment), and
        // when the recent ones were stopped.
        private String lastInsecurePage = null;
        private long lastInsecureAt = 0;
        private final ArrayDeque<Long> recentInsecureBlocks = new ArrayDeque<Long>();

        /**
         * Constructor.
         *
         * @param webView
         * @param mEditText
         */
        public InAppBrowserClient(CordovaWebView webView, EditText mEditText, String beforeload) {
            this.webView = webView;
            this.edittext = mEditText;
            this.beforeload = beforeload;
            this.waitForBeforeload = beforeload != null;
        }

        /**
         * Override the URL that should be loaded
         *
         * Legacy (deprecated in API 24)
         * For Android 6 and below.
         *
         * @param webView
         * @param url
         */
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            // OutSystems fork: no httpsonly check here, as this callback does not tell iframes from
            // the page; onPageStarted stops an http page instead.
            return shouldOverrideUrlLoading(url, null);
        }

        /**
         * Override the URL that should be loaded
         *
         * New (added in API 24)
         * For Android 7 and above.
         *
         * @param webView
         * @param request
         */
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
            // OutSystems fork: httpsonly applies to the page itself, iframes are left to the page.
            if (request.isForMainFrame() && blockInsecure(request.getUrl().toString())) {
                return true;
            }
            return shouldOverrideUrlLoading(request.getUrl().toString(), request.getMethod());
        }

        /**
         * Override the URL that should be loaded
         *
         * This handles a small subset of all the URIs that would be encountered.
         *
         * @param url
         * @param method
         */
        public boolean shouldOverrideUrlLoading(String url, String method) {
            boolean override = false;
            boolean useBeforeload = false;
            String errorMessage = null;

            if (beforeload.equals("yes") && method == null) {
                useBeforeload = true;
            } else if(beforeload.equals("yes")
                    //TODO handle POST requests then this condition can be removed:
                    && !method.equals("POST"))
            {
                useBeforeload = true;
            } else if(beforeload.equals("get") && (method == null || method.equals("GET"))) {
                useBeforeload = true;
            } else if(beforeload.equals("post") && (method == null || method.equals("POST"))) {
                //TODO handle POST requests
                errorMessage = "beforeload doesn't yet support POST requests";
            }

            // On first URL change, initiate JS callback. Only after the beforeload event, continue.
            if (useBeforeload && this.waitForBeforeload) {
                if(sendBeforeLoad(url, method)) {
                    return true;
                }
            }

            if(errorMessage != null) {
                try {
                    LOG.e(LOG_TAG, errorMessage);
                    JSONObject obj = new JSONObject();
                    obj.put("type", LOAD_ERROR_EVENT);
                    obj.put("url", url);
                    obj.put("code", -1);
                    obj.put("message", errorMessage);
                    sendUpdate(obj, true, PluginResult.Status.ERROR);
                } catch(Exception e) {
                    LOG.e(LOG_TAG, "Error sending loaderror for " + url + ": " + e.toString());
                }
            }

            if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:") || url.startsWith("intent:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    } else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        Uri uri = Uri.parse(url);
                        String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            // Test for whitelisted custom scheme names like mycoolapp:// or twitteroauthresponse:// (Twitter Oauth Response)
            else if (!url.startsWith("http:") && !url.startsWith("https:") && url.matches("^[A-Za-z0-9+.-]*://.*?$")) {
                if (allowedSchemes == null) {
                    String allowed = preferences.getString("AllowedSchemes", null);
                    if(allowed != null) {
                        allowedSchemes = allowed.split(",");
                    }
                }
                if (allowedSchemes != null) {
                    for (String scheme : allowedSchemes) {
                        if (url.startsWith(scheme)) {
                            try {
                                JSONObject obj = new JSONObject();
                                obj.put("type", "customscheme");
                                obj.put("url", url);
                                sendUpdate(obj, true);
                                override = true;
                            } catch (JSONException ex) {
                                LOG.e(LOG_TAG, "Custom Scheme URI passed in has caused a JSON error.");
                            }
                        }
                    }
                }
            }

            if (useBeforeload) {
                this.waitForBeforeload = true;
            }
            return override;
        }

        private boolean sendBeforeLoad(String url, String method) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", BEFORELOAD);
                obj.put("url", url);
                if(method != null) {
                    obj.put("method", method);
                }
                sendUpdate(obj, true);
                return true;
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
            return false;
        }

        /**
         * New (added in API 21)
         * For Android 5.0 and above.
         *
         * @param view
         * @param request
         */
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // OutSystems fork: httpsonly answers an http page itself, with an empty one, when
            // shouldOverrideUrlLoading could not cancel it (form post, back, reload, Android 6 and
            // older): nothing goes over http and no http content runs. onPageStarted then leaves it.
            if (request.isForMainFrame() && isInsecure(request.getUrl().toString())) {
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }
            return shouldInterceptRequest(request.getUrl().toString(), super.shouldInterceptRequest(view, request), request.getMethod());
        }

        public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
            return response;
        }

        /*
         * onPageStarted fires the LOAD_START_EVENT
         *
         * @param view
         * @param url
         * @param favicon
         */
        @Override
        public void onPageStarted(final WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // OutSystems fork: httpsonly, for what shouldOverrideUrlLoading does not see (POST, back,
            // reload, and before Android 7 any navigation).
            if (blockInsecure(url)) {
                WebBackForwardList history = view.copyBackForwardList();
                WebHistoryItem current = history.getCurrentItem();
                view.stopLoading();
                if (current == null || !url.equals(current.getUrl())) {
                    // Not in the history yet: the page before is still shown.
                    return;
                }
                // Back to the page before, so Back and Forward never get stuck on a blank page; a
                // blank page only when there is none. With history.back() from the page, as
                // goBack() and goBackOrForward() skip the pages that navigated without a user
                // gesture (Chromium history intervention), and may then not move.
                // Posted, once this page has committed.
                // A page that sends itself to http again as soon as it is back, like a form that
                // submits itself on load, would loop: a blank page instead when the same http page
                // comes back within INSECURE_REPEAT_MS (its query, say a CSRF token, may change),
                // or at the INSECURE_BURST_COUNT-th block within INSECURE_BURST_MS (any http page).
                long now = SystemClock.elapsedRealtime();
                String page = pageOf(url);
                while (!recentInsecureBlocks.isEmpty() && now - recentInsecureBlocks.peekFirst() >= INSECURE_BURST_MS) {
                    recentInsecureBlocks.pollFirst();
                }
                recentInsecureBlocks.addLast(now);
                boolean repeated = (page.equals(lastInsecurePage) && now - lastInsecureAt < INSECURE_REPEAT_MS)
                        || recentInsecureBlocks.size() >= INSECURE_BURST_COUNT;
                lastInsecurePage = page;
                lastInsecureAt = now;
                final boolean hasPageBefore = !repeated && history.getCurrentIndex() > 0;
                if (!hasPageBefore) {
                    blankingInsecurePage = true;
                    // From the blank page, Back to this http page goes back to the page before
                    // again, instead of to one more blank page.
                    lastInsecurePage = null;
                    lastInsecureAt = 0;
                    recentInsecureBlocks.clear();
                }
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasPageBefore) {
                            view.evaluateJavascript("history.back()", null);
                        } else {
                            view.loadUrl("about:blank");
                        }
                    }
                });
                return;
            }
            // Neither that page nor the blank one shown instead raise events: the loaderror says it all.
            if (blankingInsecurePage && "about:blank".equals(url)) {
                return;
            }
            blankingInsecurePage = false;
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            else
            {
                // Assume that everything is HTTP at this point, because if we don't specify,
                // it really should be.  Complain loudly about this!!!
                LOG.e(LOG_TAG, "Possible Uncaught/Unknown URI");
                newloc = "http://" + url;
            }

            // Update the UI if we haven't already
            if (!newloc.equals(edittext.getText().toString())) {
                edittext.setText(newloc);
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);
                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Re-arm beforeload after allowing the previously approved navigation.
            if (beforeload != null && !beforeload.isEmpty()) {
                this.waitForBeforeload = true;
            }

            // OutSystems fork: no loadstop for an http page httpsonly stopped, nor for the blank page
            // shown instead.
            if (isInsecure(url)) {
                return;
            }
            if (blankingInsecurePage && "about:blank".equals(url)) {
                blankingInsecurePage = false;
                return;
            }

            // Set the namespace for postMessage()
            injectDeferredObject("window.webkit={messageHandlers:{cordova_iab:cordova_iab}}", null);

            // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device storage
            CookieManager.getInstance().flush();

            // https://issues.apache.org/jira/browse/CB-11248
            view.clearFocus();
            view.requestFocus();

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            // Ensure future navigations can still trigger beforeload after an error.
            if (beforeload != null && !beforeload.isEmpty()) {
                this.waitForBeforeload = true;
            }

            // OutSystems fork: an http page httpsonly stopped was already reported.
            if (isInsecure(failingUrl)) {
                return;
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", error.getUrl());
                obj.put("code", 0);
                obj.put("sslerror", error.getPrimaryError());
                String message;
                switch (error.getPrimaryError()) {
                case SslError.SSL_DATE_INVALID:
                    message = "The date of the certificate is invalid";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "Hostname mismatch";
                    break;
                default:
                case SslError.SSL_INVALID:
                    message = "A generic error occurred";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid";
                    break;
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted";
                    break;
                }
                obj.put("message", message);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
            handler.cancel();
        }

        /**
         * On received http auth request.
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

            // Check if there is some plugin which can resolve this auth challenge
            PluginManager pluginManager = null;
            try {
                Method gpm = webView.getClass().getMethod("getPluginManager");
                pluginManager = (PluginManager)gpm.invoke(webView);
            } catch (NoSuchMethodException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (InvocationTargetException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            }

            if (pluginManager == null) {
                try {
                    Field pmf = webView.getClass().getField("pluginManager");
                    pluginManager = (PluginManager)pmf.get(webView);
                } catch (NoSuchFieldException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                } catch (IllegalAccessException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                }
            }

            if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(webView, new CordovaHttpAuthHandler(handler), host, realm)) {
                return;
            }

            // By default handle 401 like we'd normally do!
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }
}
