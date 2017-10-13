package com.spid.android.sdk.request;

import android.os.AsyncTask;

import com.spid.android.sdk.SPiDClient;
import com.spid.android.sdk.accesstoken.SPiDAccessToken;
import com.spid.android.sdk.exceptions.SPiDException;
import com.spid.android.sdk.listener.SPiDRequestListener;
import com.spid.android.sdk.logger.SPiDLogger;
import com.spid.android.sdk.response.SPiDResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Contains a request to SPiD, note that each request can only be used once since it extends <code>AsyncTask</code>
 */
public class SPiDRequest extends AsyncTask<Void, Void, SPiDResponse> {

    public static final String GET = "GET";
    public static final String POST = "POST";

    private static final Integer DEFAULT_MAX_RETRY_COUNT = 0;
    private final String method;

    protected final SPiDRequestListener listener;

    private String url;

    private Map<String, String> headers;
    private Map<String, String> query;
    private Map<String, String> body;
    private Integer retryCount;
    private Integer maxRetryCount;

    /**
     * Constructor for the SPiDRequest
     *
     * @param method   The http method to be used
     * @param url      The request url
     * @param listener Called on completion or error, can be <code>null</code>
     */
    public SPiDRequest(String method, String url, SPiDRequestListener listener) {
        super();
        this.url = url;
        this.method = method;
        this.headers = new HashMap<>();
        this.query = new HashMap<>();
        this.body = new HashMap<>();

        this.listener = listener;

        this.retryCount = 0;
        this.maxRetryCount = DEFAULT_MAX_RETRY_COUNT;

        SPiDLogger.log("Created request: " + url);
    }

    /**
     * Constructor for the SPiDRequest, sets default method to GET
     *
     * @param url      The request url
     * @param listener Called on completion or error, can be <code>null</code>
     */
    public SPiDRequest(String url, SPiDRequestListener listener) {
        this(GET, url, listener);
    }

    /**
     * @return The http method for the request
     */
    public String getMethod() {
        return method;
    }

    /**
     * Adds a key/value to the query
     *
     * @param key   The key
     * @param value The value
     */
    public void addQueryParameter(String key, String value) {
        query.put(key, value);
    }

    /**
     * Adds a key/value to the body
     *
     * @param key   The key
     * @param value The value
     */
    public void addBodyParameter(String key, String value) {
        body.put(key, value);
    }

    /**
     * @return The complete URL with the query
     */
    public String getCompleteURL() {
        return url + getQueryAsString();
    }

    /**
     * Encodes a key/value to "key=value" for use in the query string
     *
     * @param key   The key to be encoded
     * @param value The value to be encoded
     * @return The encoded string
     */
    private String encodeURLParameter(String key, String value) {
        try {
            return String.format("%s=%s", URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return String.format("%s=%s", key, value);
        }
    }

    /**
     * Generates the query string
     *
     * @return The query
     */
    private String getQueryAsString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (builder.length() > 0)
                builder.append('&');
            else
                builder.append('?');
            builder.append(encodeURLParameter(entry.getKey(), entry.getValue()));
        }
        return builder.toString();
    }

    /**
     * Creates a copy of the <code>SPiDRequest</code>, this is used since AsyncTask can only be used once
     *
     * @return A copy of the <code>SPiDRequest</code>
     */
    private SPiDRequest copy() {
        SPiDRequest request = new SPiDRequest(method, url, listener);
        request.setRetryCount(retryCount);
        request.setHeaders(headers);
        request.setQuery(query);
        request.setBody(body);
        return request;
    }

    /**
     * @param headers The http headers
     */
    private void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * @param query The query parameters
     */
    private void setQuery(Map<String, String> query) {
        this.query = query;
    }

    /**
     * @param body The http body
     */
    private void setBody(Map<String, String> body) {
        this.body = body;
    }

    /**
     * Runs the request and receives a response in a background thread.
     *
     * @param voids Unused parameter required by <code>AsyncTask</code>
     * @return A <code>SPiDResponse</code>
     */
    @Override
    protected SPiDResponse doInBackground(Void... voids) {
        OkHttpClient client = SPiDClient.getInstance().getHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        Call httpCall = null;

        try {
            if (POST.equalsIgnoreCase(method)) {
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (Map.Entry<String, String> entry : body.entrySet()) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
                requestBuilder.url(url);
                requestBuilder.post(formBuilder.build());
            } else {
                requestBuilder.url(url + getQueryAsString());
            }

            // Add custom User-Agent
            requestBuilder.addHeader("User-Agent", SPiDClient.getInstance().getConfig().getUserAgent());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            httpCall = client.newCall(requestBuilder.build());
            Response httpResponse = httpCall.execute();
            return new SPiDResponse(httpResponse);
        } catch (IOException e) {
            return new SPiDResponse(e);
        } catch (Exception e) {
            return new SPiDResponse(e);
        } finally {
            if (httpCall != null) {
                httpCall.cancel();
            }
        }
    }

    /**
     * Runs on the UI thread after doInBackground
     *
     * @param response The <code>SPiDResponse</code> created in doInBackground
     */
    @Override
    protected void onPostExecute(SPiDResponse response) {
        super.onPostExecute(response);
        doOnPostExecute(response);
    }

    /**
     * Checks the <code>SPiDResponse</code> for errors, handles retries and invokes the callback listener
     *
     * @param response The <code>SPiDResponse</code> created in doInBackground
     */
    protected void doOnPostExecute(SPiDResponse response) {
        Exception exception = response.getException();
        if (exception != null) {
            if (exception instanceof IOException) {
                listener.onError(exception);
            } else if (exception instanceof SPiDException) {
                String error = ((SPiDException) exception).getError();
                if (SPiDException.EXPIRED_TOKEN.equals(error) || SPiDException.INVALID_TOKEN.equals(error)) {
                    if (retryCount < maxRetryCount) {
                        SPiDRequest request = this.copy();
                        request.increaseRetryCount();
                        SPiDClient.getInstance().addWaitingRequest(request);
                        SPiDClient.getInstance().refreshAccessToken(null);
                        SPiDLogger.log("Retrying attempt: " + request.retryCount + " for request: " + request.url);
                    } else {
                        SPiDClient.getInstance().clearAccessToken();
                        listener.onError(exception);
                    }
                } else {
                    listener.onError(exception);
                }
            } else {
                listener.onError(exception);
            }
        } else {
            listener.onComplete(response);
        }
    }

    /**
     * Execute request, can only be called once
     */
    public void execute() {
        execute((Void) null);
    }

    /**
     * Execute request authorized request, appends oauth token if needed
     */
    public void executeAuthorizedRequest() {
        SPiDAccessToken accessToken = SPiDClient.getInstance().getAccessToken();
        if (method.equals(GET)) {
            if (!url.contains(SPiDClient.OAUTH_TOKEN) && accessToken != null) {
                url = url.contains("?") ? url + "&" : url + "?";
                url = url + SPiDClient.OAUTH_TOKEN + "=" + accessToken.getAccessToken();
            }
        } else { // POST
            if (!body.containsKey(SPiDClient.OAUTH_TOKEN) && accessToken != null) {
                addBodyParameter(SPiDClient.OAUTH_TOKEN, accessToken.getAccessToken());
            }
        }
        execute();
    }

    /**
     * Maximum retry count for the request, used when token expires and the request needs to be retried after a new token has been obtained.
     *
     * @param maxRetryCount Max retry attempts
     */
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Increases the retryCount by 1
     */
    public void increaseRetryCount() {
        retryCount++;
    }
}
