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

    // ── Kafka ─────────────────────────────────────────────────────────
    public static final String KAFKA_GROUP_ID                = "chitchat";
    public static final String KAFKA_STREAMS_APP_ID          = "chitchat-presence";
    public static final String KAFKA_TOPIC_CHAT_MESSAGES     = "chat.messages";
    public static final String KAFKA_TOPIC_PRESENCE_STATE    = "presence.state";
    public static final String KAFKA_TOPIC_NOTIFICATIONS     = "notifications";

    // ── WebSocket Destinations ────────────────────────────────────────
    public static final String WS_TOPIC_ROOMS       = "/topic/rooms/";
    public static final String WS_QUEUE_MESSAGES    = "/queue/messages";
    public static final String WS_QUEUE_NOTIFICATIONS = "/queue/notifications";
    public static final String WS_TOPIC_ROOMS_PRESENCE_SUFFIX = "/presence";
}
