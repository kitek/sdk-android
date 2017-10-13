package com.spid.android.sdk.configuration;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.spid.android.sdk.BuildConfig;
import com.spid.android.sdk.logger.SPiDLogger;

import okhttp3.OkHttpClient;

/**
 * Builder class for SPiDConfiguration
 */
public class SPiDConfigurationBuilder {

    private Context context;
    private OkHttpClient httpClient;
    private SPiDEnvironment spidEnvironment;
    private Boolean debugMode = Boolean.FALSE;

    private String clientID;
    private String clientSecret;
    private String signSecret;
    private String appURLScheme;
    private String redirectURL;
    private String authorizationURL;
    private String signupURL;
    private String forgotPasswordURL;
    private String tokenURL;
    private String serverClientID;
    private String serverRedirectUri;
    private String apiVersion = "2";

    public SPiDConfigurationBuilder(Context context, SPiDEnvironment spidEnvironment, String clientID,
                                    String clientSecret, String appURLScheme) {
        this.context = context;
        this.spidEnvironment = spidEnvironment;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.appURLScheme = appURLScheme;
    }

    /**
     * @param signSecret SPiD sign secret
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder signSecret(String signSecret) {
        this.signSecret = signSecret;
        return this;
    }

    /**
     * @param appURLScheme Android app url scheme
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder appURLScheme(String appURLScheme) {
        this.appURLScheme = appURLScheme;
        return this;
    }

    /**
     * @param redirectURL SPiD redirect url
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder redirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
        return this;
    }

    /**
     * @param authorizationURL SPiD authorization url
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder authorizationURL(String authorizationURL) {
        this.authorizationURL = authorizationURL;
        return this;
    }

    /**
     * @param registrationURL SPiD registration url
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder registrationURL(String registrationURL) {
        this.signupURL = registrationURL;
        return this;
    }

    /**
     * @param forgotPasswordURL SPiD lost password url
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder forgotPasswordURL(String forgotPasswordURL) {
        this.forgotPasswordURL = forgotPasswordURL;
        return this;
    }

    /**
     * @param tokenURL SPiD token url
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder tokenURL(String tokenURL) {
        this.tokenURL = tokenURL;
        return this;
    }

    /**
     * @param serverClientID SPiD client id for server
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder serverClientID(String serverClientID) {
        this.serverClientID = serverClientID;
        return this;
    }

    /**
     * @param serverRedirectUri SPiD redirect uri for server
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder serverRedirectUri(String serverRedirectUri) {
        this.serverRedirectUri = serverRedirectUri;
        return this;
    }

    public SPiDConfigurationBuilder httpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * @param apiVersion SPiD API version
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * @param debugMode Use debug mode, default is <code>false</code>
     * @return The SPiDConfigurationBuilder
     */
    public SPiDConfigurationBuilder debugMode(Boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    /**
     * Checks that supplied string is not empty, otherwise throws exception
     *
     * @param string       The string to check
     * @param errorMessage Error message for the exception
     */
    protected void isEmptyString(String string, String errorMessage) {
        if (string == null || TextUtils.isEmpty(string.trim())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks if supplied object is not null, otherwise throws exception
     *
     * @param object       The object to check
     * @param errorMessage Error message for the exception
     * @throws IllegalArgumentException Thrown if object is null
     */
    protected void isNull(Object object, String errorMessage) {
        if (object == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Setup custom User-Agent for all SPiD requests
     *
     * @return Custom User-Agent
     */

    private String getUserAgent() {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        PackageInfo packageInfo = null;
        try {
            if (packageManager != null) {
                applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
                packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            }
        } catch (final PackageManager.NameNotFoundException e) {
            SPiDLogger.log("Could not get package info");
        }

        String applicationName = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "UnknownApplication");
        String applicationVersion = packageInfo != null ? packageInfo.versionName : "UnknownVersion";

        return applicationName + "/" + applicationVersion + " " + "SPiDAndroidSDK/" + BuildConfig.VERSION_NAME + " " + "Android/" + android.os.Build.MODEL + "/API " + Build.VERSION.SDK_INT;
    }

    /**
     * Builds a SPiDConfiguration object from the supplied values. It also check all that all mandatory values are set and generates default values for non-mandatory values that are missing.
     *
     * @return A SPiDConfiguration object
     */
    public SPiDConfiguration build() {
        isEmptyString(clientID, "ClientID is missing");
        isEmptyString(clientSecret, "ClientSecret is missing");
        isEmptyString(appURLScheme, "AppURLScheme is missing");
        if (spidEnvironment == null) {
            throw new IllegalStateException("SPiDEnvironment not set");
        }
        isNull(context, "Context is missing");

        if (redirectURL == null || TextUtils.isEmpty(redirectURL.trim())) {
            redirectURL = appURLScheme + "://";
        }

        if (authorizationURL == null || TextUtils.isEmpty(authorizationURL.trim())) {
            authorizationURL = spidEnvironment.toString() + "/flow/login";
        }

        if (tokenURL == null || TextUtils.isEmpty(tokenURL.trim())) {
            tokenURL = spidEnvironment.toString() + "/oauth/token";
        }

        if (signupURL == null || TextUtils.isEmpty(signupURL.trim())) {
            signupURL = spidEnvironment.toString() + "/flow/signup";
        }

        if (forgotPasswordURL == null || TextUtils.isEmpty(forgotPasswordURL.trim())) {
            String forgotPasswordBaseUrl = spidEnvironment.toString() + "/flow/password";

            Uri forgotPasswordUri = Uri.parse(forgotPasswordBaseUrl)
                    .buildUpon()
                    .appendQueryParameter("client_id", clientID)
                    .appendQueryParameter("redirect_uri", redirectURL)
                    .build();
            forgotPasswordURL = forgotPasswordUri.toString();
        }

        if (serverClientID == null || TextUtils.isEmpty(serverClientID.trim())) {
            serverClientID = clientID;
        }

        if (serverRedirectUri == null || TextUtils.isEmpty(serverRedirectUri.trim())) {
            serverRedirectUri = redirectURL;
        }

        String userAgent = getUserAgent();
        if (httpClient == null) {
            httpClient = new OkHttpClient();
        }

        return new SPiDConfiguration(
                clientID,
                clientSecret,
                signSecret,
                appURLScheme,
                spidEnvironment,
                redirectURL,
                authorizationURL,
                signupURL,
                forgotPasswordURL,
                tokenURL,
                serverClientID,
                serverRedirectUri,
                apiVersion,
                debugMode,
                userAgent,
                context,
                httpClient
        );
    }
}
