package com.bondhub.friendservice.dto.response;

import lombok.Builder;

@Builder
public record UserSummaryResponse(
    String id,
    String accountId,
    String userName,
    String email,
    String phone,
    String avatar
) {}
