package com.chitchat.app.graphql.query;

import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.security.JwtTokenProvider;
import com.chitchat.app.service.SessionService;
import com.chitchat.app.util.HttpUtil;
import com.chitchat.app.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SessionQueryResolver {

    private final SessionService sessionService;
    private final JwtTokenProvider jwtTokenProvider;

    @QueryMapping
    public List<SessionResponse> mySessions() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = HttpUtil.extractBearerToken(request);
        String jti = jwtTokenProvider.getJtiFromToken(token);
        return sessionService.getActiveSessions(SecurityUtil.getCurrentUserId(), jti);
    }
}
