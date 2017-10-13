package com.spid.android.sdk.response;

import android.text.TextUtils;

import com.spid.android.sdk.exceptions.SPiDException;
import com.spid.android.sdk.exceptions.SPiDInvalidResponseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Response;

/**
 * Contains a response from SPiD
 */
public class SPiDResponse {

    private final Integer code;
    private final Map<String, String> headers;

    private String body;
    private JSONObject jsonObject;
    private Exception exception;

    /**
     * Constructor for SPiDResponse
     *
     * @param exception exception
     */
    public SPiDResponse(Exception exception) {
        this.code = SPiDException.UNKNOWN_CODE;
        this.body = "";
        this.headers = new HashMap<>();
        this.exception = exception;
    }

    /**
     * Constructor for SPiDResponse
     *
     * @param httpResponse The response from SPiD
     */
    public SPiDResponse(Response httpResponse) throws IOException {
        code = httpResponse.code();
        headers = new HashMap<>();
        exception = null;

        Headers responseHeaders = httpResponse.headers();
        for (int i = 0; i < responseHeaders.size(); i++) {
            headers.put(responseHeaders.name(i), responseHeaders.value(i));
        }

        body = httpResponse.body().string();

        if (!TextUtils.isEmpty(body)) {
            try {
                this.jsonObject = new JSONObject(this.body);
                if (jsonObject.has("error") && !("null".equals(jsonObject.getString("error")))) {
                    exception = SPiDException.create(jsonObject);
                }
            } catch (JSONException e) {
                jsonObject = new JSONObject();
                exception = new SPiDInvalidResponseException("Invalid response from SPiD: " + body);
            }
        } else {
            jsonObject = new JSONObject();
        }

        if (!isSuccessful()) {
            exception = SPiDException.create(jsonObject);
        }
    }

    /**
     * @return If request was successful, i.e. http code between 200 and 400
     */
    public boolean isSuccessful() {
        return getCode() >= HttpURLConnection.HTTP_OK && getCode() < HttpURLConnection.HTTP_BAD_REQUEST;
    }

    /**
     * @return The http status code
     */
    public int getCode() {
        return code != null ? code : SPiDException.UNKNOWN_CODE;
    }

    /**
     * @return The http body
     */
    public String getBody() {
        return body;
    }

    /**
     * @return The http body as a <code>JSONObject</code>
     */
    public JSONObject getJsonObject() {
        return jsonObject;
    }

    /**
     * @return Exception if there was any otherwise <code>null</code>
     */
    public Exception getException() {
        return exception;
    }
}
