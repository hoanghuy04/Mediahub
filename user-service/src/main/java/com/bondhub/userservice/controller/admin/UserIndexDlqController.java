package com.bondhub.userservice.controller.admin;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.common.dto.PageResponse;
import com.bondhub.common.dto.OutboxResponse;
import com.bondhub.common.model.kafka.EventType;
import com.bondhub.common.model.kafka.OutboxEvent;
import com.bondhub.common.utils.LocalizationUtil;
import com.bondhub.userservice.dto.request.elasticsearch.DeadEventRetryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import com.bondhub.userservice.service.elasticsearch.UserDeadEventService;

@RestController
@RequestMapping("/users/elasticsearch/dlq")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserIndexDlqController {

    private final UserDeadEventService userDeadEventService;
    private final LocalizationUtil localizationUtil;

    @PostMapping("/retry")
    public ApiResponse<Map<String, Object>> retryDeadEvents(@RequestBody(required = false) DeadEventRetryRequest request) {
        int count = userDeadEventService.retryDeadEvents(request);
        return ApiResponse.success(Map.of(
            "message", localizationUtil.getMessage("search.retry.dead.success", count),
            "count", count
        ));
    }



    @GetMapping("/paged")
    public ApiResponse<PageResponse<List<OutboxResponse>>> getDeadEventsPaged(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime toDate,
            @PageableDefault(size = 10, sort = "lastModifiedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(userDeadEventService.getDeadEventsPaged(
                search, eventType, fromDate, toDate, pageable));
    }
}
