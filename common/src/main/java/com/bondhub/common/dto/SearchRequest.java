package com.bondhub.common.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchRequest {
    String searchTerm;
    List<String> searchFields;
    Map<String, String> termFilters;
    Map<String, RangeFilter> rangeFilters;
    String fuzziness;
    @Builder.Default
    int page = 0;
    @Builder.Default
    int size = 10;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RangeFilter {
        String from;
        String to;
    }
}
