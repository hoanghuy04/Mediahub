package com.bondhub.userservice.dto.request.elasticsearch;

import java.time.ZonedDateTime;

public record DeadEventRetryRequest(
    ZonedDateTime fromDate,
    ZonedDateTime toDate
) {}
