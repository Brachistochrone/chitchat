package com.chitchat.app.graphql.query;

import com.chitchat.app.dto.response.UserResponse;
import com.chitchat.app.service.UserService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserQueryResolver {

    private final UserService userService;

    @QueryMapping
    public UserResponse me() {
        return userService.getMe(SecurityUtil.getCurrentUserId());
    }

    @QueryMapping
    public UserResponse user(@Argument String username) {
        return userService.getUserByUsername(username);
    }
}
