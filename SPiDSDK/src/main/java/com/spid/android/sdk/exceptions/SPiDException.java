package com.spid.android.sdk.exceptions;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class for all exceptions in SPiD.
 */
public class SPiDException extends RuntimeException {

    public static final String REDIRECT_URI_MISMATCH = "redirect_uri_mismatch";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_CLIENT_ID = "invalid_client_id"; // Replaced by "invalid_client" in draft 10 of oauth 2.0
    public static final String INVALID_CLIENT_CREDENTIALS = "invalid_client_credentials"; // Replaced by "invalid_client" in draft 10 of oauth 2.0
    public static final String INVALID_TOKEN = "invalid_token";
    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";
    public static final String EXPIRED_TOKEN = "expired_token";
    public static final String UNVERIFIED_USER = "unverified_user";

    public static final String UNKNOWN_USER = "unknown_user";
    public static final String INVALID_USER_CREDENTIALS = "invalid_user_credentials";

    private static final String API_EXCEPTION = "ApiException";
    private static final String OAUTH_EXCEPTION = "OAuthException";
    private static final String SPID_EXCEPTION = "SPiDException";

    public static final Integer UNKNOWN_CODE = -1;

    private String error;
    private Integer errorCode;
    private String errorType;
    private Map<String, String> descriptions;

    /**
     * Constructs a new SPiDException with the specified detail message.
     *
     * @param message The detail message.
     */
    public SPiDException(String message) {
        super(message);
        initDefaultValues(message);
    }

    /**
     * Constructs a new SPiDException with the specified cause and a detail message of (cause==null ? null : cause.toString()) (which typically contains the class and detail message of cause).
     *
     * @param cause The cause
     */
    public SPiDException(Throwable cause) {
        super(cause);
        initDefaultValues(cause.getMessage());
    }

    /**
     * Constructs a new SPiDException with the specified detail message and cause.
     *
     * @param message   The detail message
     * @param throwable The cause
     */
    public SPiDException(String message, Throwable throwable) {
        super(message, throwable);
        initDefaultValues(message);
    }

    /**
     * Constructs a new SPiDException with the specified error, description, errorCode and type.
     *
     * @param error       The error as a string, see predefined constants in this class
     * @param description The detail message
     * @param errorCode   The error code
     * @param type        The error type
     */
    public SPiDException(String error, String description, Integer errorCode, String type) {
        super(description);
        this.error = error;
        this.errorCode = errorCode;
        this.errorType = type;
        this.descriptions = new HashMap<String, String>();
        this.descriptions.put("error", description);
    }

    /**
     * Constructs a new SPiDException with the specified error, description, errorCode and type.
     *
     * @param error        The error as a string, see predefined constants in this class
     * @param descriptions The detail messages
     * @param errorCode    The error code
     * @param type         The error type
     */
    public SPiDException(String error, Map<String, String> descriptions, Integer errorCode, String type) {
        super(descriptions.containsKey("error") ? descriptions.get("error") : descriptions.toString());
        this.error = error;
        this.errorCode = errorCode;
        this.errorType = type;
        this.descriptions = descriptions;
    }

    private void initDefaultValues(String message) {
        this.error = SPID_EXCEPTION;
        this.errorCode = UNKNOWN_CODE;
        this.errorType = SPID_EXCEPTION;
        this.descriptions = new HashMap<String, String>();
        this.descriptions.put("error", message);
    }

    /**
     * Creates a SPiDException from a JSONObject
     *
     * @param data The JSONObject that contains the error
     * @return The generated exception
     */
    public static SPiDException create(JSONObject data) {
        String error;
        String errorCodeString;
        String type;
        Map<String, String> descriptions = new HashMap<String, String>();

        JSONObject errorObject = data.optJSONObject("error");
        if (errorObject != null) {
            error = errorObject.optString("error");
            errorCodeString = errorObject.optString("code");
            type = errorObject.optString("type");

            JSONObject descriptionsJson = errorObject.optJSONObject("description");
            if (descriptionsJson != null) {
                descriptions = descriptionsFromJSONObject(descriptionsJson);
            } else {
                descriptions.put("error", errorObject.optString("description", "Missing error description"));
            }
        } else {
            error = data.optString("error");
            errorCodeString = data.optString("error_code");
            type = data.optString("type");
            descriptions.put("error", data.optString("error_description", "Missing error description"));
        }

        if (TextUtils.isEmpty(error) && !TextUtils.isEmpty(type)) {
            error = type;
        }

        if (descriptions.isEmpty()) {
            descriptions.put("error", type);
        }

        Integer errorCode;
        try {
            errorCode = Integer.valueOf(errorCodeString);
        } catch (NumberFormatException e) {
            errorCode = SPiDException.UNKNOWN_CODE;
        }

        type = TextUtils.isEmpty(type) ? SPID_EXCEPTION : type;

        if (API_EXCEPTION.equals(type)) {
            return new SPiDApiException(error, descriptions, errorCode, type);
        } else if (INVALID_TOKEN.equals(error) || EXPIRED_TOKEN.equals(error)) {
            return new SPiDInvalidAccessTokenException(error, descriptions, errorCode, type);
        } else if (UNKNOWN_USER.equals(error)) {
            return new SPiDUnknownUserException(error, descriptions, errorCode, type);
        } else if (OAUTH_EXCEPTION.equals(type)) {
            if (UNVERIFIED_USER.equals(error)) {
                return new SPiDUnverifiedUserException(error, descriptions, errorCode, type);
            } else {
                return new SPiDOAuthException(error, descriptions, errorCode, type);
            }
        } else {
            return new SPiDException(error, descriptions, errorCode, SPID_EXCEPTION);
        }
    }

    /**
     * Extracts error description for <code>JSONObject</code>
     *
     * @return Descriptions as a <code>Map</code>
     */
    private static Map<String, String> descriptionsFromJSONObject(JSONObject jsonObject) {
        Map<String, String> map = new HashMap<String, String>();
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, jsonObject.optString(key, "Missing description details"));
        }
        return map;
    }

    /**
     * @return The error as a string, see predefined constants in this class
     */
    public String getError() {
        return error;
    }

    /**
     * @return The error code
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * @return The error type
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @return Error descriptions
     */
    public Map<String, String> getDescriptions() {
        return descriptions;
    }
}
