package com.bondhub.friendservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record FriendRequestActionRequest(
    @NotBlank(message = "{friend.request.friendshipId.notBlank}")
    String friendshipId
) {}
