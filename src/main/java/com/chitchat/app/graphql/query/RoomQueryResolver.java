package com.chitchat.app.graphql.query;

import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.dto.response.RoomPage;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.service.RoomService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoomQueryResolver {

    private final RoomService roomService;

    @QueryMapping
    public RoomPage rooms(@Argument String query, @Argument Integer page, @Argument Integer size) {
        String q = query != null ? query : "";
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;
        Page<RoomResponse> result = roomService.searchPublicRooms(q, p, s);
        return RoomPage.builder()
                .items(result.getContent())
                .totalCount((int) result.getTotalElements())
                .build();
    }

    @QueryMapping
    public RoomResponse room(@Argument Long id) {
        return roomService.getRoom(id, SecurityUtil.getCurrentUserId());
    }

    @QueryMapping
    public List<MemberResponse> roomMembers(@Argument Long roomId) {
        return roomService.getMembers(roomId, SecurityUtil.getCurrentUserId());
    }

    @QueryMapping
    public List<BanResponse> roomBans(@Argument Long roomId) {
        return roomService.getBans(roomId, SecurityUtil.getCurrentUserId());
    }
}
