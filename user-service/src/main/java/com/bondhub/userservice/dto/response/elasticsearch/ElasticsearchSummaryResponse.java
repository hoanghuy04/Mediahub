package com.bondhub.userservice.dto.response.elasticsearch;

import lombok.Builder;

@Builder
public record ElasticsearchSummaryResponse(
        ElasticsearchHealthResponse health,
        IndexStatsResponse stats,
        DataComparisonResponse compare,
        long deadEventsCount
) {}
