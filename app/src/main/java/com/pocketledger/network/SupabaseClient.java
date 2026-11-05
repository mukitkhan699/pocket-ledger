package com.pocketledger.network;

import com.pocketledger.models.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static volatile SupabaseClient instance;

    private final OkHttpClient client;

    public static SupabaseClient get() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    private SupabaseClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static class ApiException extends Exception {
        public final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    public static class UnauthorizedException extends ApiException {
        public UnauthorizedException(String message) {
            super(401, message);
        }
    }

    public static class AuthResult {
        public Session session;
        public boolean requiresEmailConfirmation;
        public String message;
    }

    public Session signIn(String email, String password) throws IOException, ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            throw new ApiException(0, "Could not build sign-in request");
        }
        HttpUrl url = HttpUrl.parse(SupabaseConfig.authUrl() + "/token")
                .newBuilder()
                .addQueryParameter("grant_type", "password")
                .build();
        JSONObject response = postJson(url, body, null, false);
        Session session = parseSession(response);
        if (session == null) {
            throw new ApiException(0, "Invalid authentication response");
        }
        return session;
    }

    public AuthResult signUp(String email, String password, String fullName) throws IOException, ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            JSONObject data = new JSONObject();
            data.put("full_name", fullName);
            body.put("data", data);
        } catch (Exception e) {
            throw new ApiException(0, "Could not build registration request");
        }
        HttpUrl url = HttpUrl.parse(SupabaseConfig.authUrl() + "/signup");
        JSONObject response = postJson(url, body, null, false);
        AuthResult result = new AuthResult();
        Session session = parseSession(response);
        if (session != null) {
            if (session.fullName == null || session.fullName.isEmpty()) {
                session.fullName = fullName;
            }
            result.session = session;
        } else {
            result.requiresEmailConfirmation = true;
            result.message = "Check your email to confirm your account, then log in.";
        }
        return result;
    }

    public Session refresh(String refreshToken) throws IOException, ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (Exception e) {
            throw new ApiException(0, "Could not build refresh request");
        }
        HttpUrl url = HttpUrl.parse(SupabaseConfig.authUrl() + "/token")
                .newBuilder()
                .addQueryParameter("grant_type", "refresh_token")
                .build();
        JSONObject response = postJson(url, body, null, false);
        Session session = parseSession(response);
        if (session == null) {
            throw new UnauthorizedException("Session could not be refreshed");
        }
        return session;
    }

    public void recover(String email) throws IOException, ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
        } catch (Exception e) {
            throw new ApiException(0, "Could not build recovery request");
        }
        HttpUrl url = HttpUrl.parse(SupabaseConfig.authUrl() + "/recover");
        postJson(url, body, null, false);
    }

    public void logout(String accessToken) throws IOException, ApiException {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.authUrl() + "/logout");
        Request request = baseRequest(url, accessToken)
                .post(RequestBody.create("{}", JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 401) {
                throw new ApiException(response.code(), readError(response));
            }
        }
    }

    public JSONArray select(String table, Map<String, String> params, String accessToken)
            throws IOException, ApiException {
        HttpUrl.Builder builder = HttpUrl.parse(SupabaseConfig.restUrl() + "/" + table).newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        Request request = baseRequest(builder.build(), accessToken)
                .get()
                .addHeader("Accept", "application/json")
                .build();
        return executeArray(request);
    }

    public JSONArray upsert(String table, JSONArray rows, String accessToken)
            throws IOException, ApiException {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.restUrl() + "/" + table)
                .newBuilder()
                .addQueryParameter("on_conflict", "id")
                .build();
        Request request = baseRequest(url, accessToken)
                .post(RequestBody.create(rows.toString(), JSON))
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .build();
        return executeArray(request);
    }

    private JSONObject postJson(HttpUrl url, JSONObject body, String accessToken, boolean auth)
            throws IOException, ApiException {
        Request.Builder builder = baseRequest(url, accessToken)
                .post(RequestBody.create(body.toString(), JSON));
        if (auth && accessToken != null) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    throw new UnauthorizedException(errorFromBody(text, "Unauthorized"));
                }
                throw new ApiException(response.code(), errorFromBody(text, "Request failed"));
            }
            if (text.isEmpty()) {
                return new JSONObject();
            }
            try {
                return new JSONObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }

    private JSONArray executeArray(Request request) throws IOException, ApiException {
        try (Response response = client.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    throw new UnauthorizedException(errorFromBody(text, "Unauthorized"));
                }
                throw new ApiException(response.code(), errorFromBody(text, "Request failed"));
            }
            try {
                return new JSONArray(text == null || text.isEmpty() ? "[]" : text);
            } catch (Exception e) {
                throw new ApiException(0, "Invalid response from server");
            }
        }
    }

    private Request.Builder baseRequest(HttpUrl url, String accessToken) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey())
                .addHeader("X-Client-Info", "pocket-ledger-android/1.0");
        if (accessToken != null && !accessToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
        }
        return builder;
    }

    private Session parseSession(JSONObject response) {
        try {
            String accessToken = response.optString("access_token", "");
            String refreshToken = response.optString("refresh_token", "");
            JSONObject sessionObject = response.optJSONObject("session");
            if ((accessToken == null || accessToken.isEmpty()) && sessionObject != null) {
                accessToken = sessionObject.optString("access_token", "");
                refreshToken = sessionObject.optString("refresh_token", "");
                response = sessionObject;
            }
            if (accessToken == null || accessToken.isEmpty()) {
                return null;
            }
            Session session = new Session();
            session.accessToken = accessToken;
            session.refreshToken = refreshToken;
            long expiresIn = response.optLong("expires_in", 3600L);
            session.expiresAtEpochSeconds = System.currentTimeMillis() / 1000L + expiresIn;
            JSONObject user = response.optJSONObject("user");
            if (user == null && sessionObject != null) {
                user = sessionObject.optJSONObject("user");
            }
            if (user != null) {
                session.userId = user.optString("id", "");
                session.email = user.optString("email", "");
                JSONObject metadata = user.optJSONObject("user_metadata");
                if (metadata != null) {
                    session.fullName = metadata.optString("full_name", "");
                }
            }
            if (session.userId == null || session.userId.isEmpty()) {
                return null;
            }
            return session;
        } catch (Exception e) {
            return null;
        }
    }

    private String readError(Response response) {
        try {
            ResponseBody body = response.body();
            return errorFromBody(body == null ? "" : body.string(), "Request failed");
        } catch (Exception e) {
            return "Request failed";
        }
    }

    private String errorFromBody(String body, String fallback) {
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject json = new JSONObject(body);
            String[] keys = {"error_description", "msg", "message", "error"};
            for (String key : keys) {
                String value = json.optString(key, "");
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
