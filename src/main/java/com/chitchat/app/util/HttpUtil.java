package com.chitchat.app.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class HttpUtil {

    private HttpUtil() {}

    /**
     * Extracts the raw JWT token from the {@code Authorization: Bearer <token>} header.
     *
     * @return token string, or {@code null} if the header is absent or malformed
     */
    public static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(AppConstants.BEARER_PREFIX)) {
            return header.substring(AppConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}
