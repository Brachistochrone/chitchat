package com.chitchat.app.util;

public final class AppConstants {

    private AppConstants() {}

    // ── HTTP Headers ──────────────────────────────────────────────────
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_AGENT_HEADER    = "User-Agent";
    public static final String BEARER_PREFIX        = "Bearer ";

    // ── Security ──────────────────────────────────────────────────────
    public static final String ROLE_USER            = "ROLE_USER";
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    // ── API Paths ─────────────────────────────────────────────────────
    public static final String API_AUTH_PATH        = "/api/auth/**";
    public static final String SWAGGER_UI_PATH      = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML_PATH = "/swagger-ui.html";
    public static final String API_DOCS_PATH        = "/v3/api-docs/**";
    public static final String WEBSOCKET_PATH       = "/ws/**";

    // ── WebSocket Destinations ────────────────────────────────────────
    public static final String WS_TOPIC_ROOMS       = "/topic/rooms/";
    public static final String WS_QUEUE_MESSAGES    = "/queue/messages";
}
