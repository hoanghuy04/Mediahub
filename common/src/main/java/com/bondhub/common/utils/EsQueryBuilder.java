package com.bondhub.common.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.bondhub.common.dto.SearchRequest;
import com.bondhub.common.dto.SearchRequest.RangeFilter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EsQueryBuilder {

    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public static NativeQuery buildNativeQuery(
            SearchRequest request,
            String defaultField,
            Consumer<BoolQuery.Builder> customCriteria) {

        if (request == null || !StringUtils.hasText(request.getSearchTerm())) {
            return null;
        }

        Query query = buildQuery(request, defaultField, customCriteria);

        Pageable pageable = PageRequest.of(
                Math.max(request.getPage(), 0),
                Math.max(request.getSize(), 10),
                Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc(DEFAULT_SORT_FIELD)
                )
        );

        HighlightQuery highlightQuery = buildHighlightQuery(request.getSearchFields(), defaultField);

        return NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable)
                .withHighlightQuery(highlightQuery)
                .build();
    }

    private static Query buildQuery(SearchRequest request, String defaultField, Consumer<BoolQuery.Builder> customCriteria) {
        Query textQuery = buildTextQuery(request.getSearchTerm(), request.getSearchFields(), defaultField, request.getFuzziness());

        List<Query> filters = new ArrayList<>();
        filters.addAll(buildTermFilters(request.getTermFilters()));
        filters.addAll(buildRangeFilters(request.getRangeFilters()));

        return Query.of(q -> q.bool(b -> {
            b.must(textQuery);
            if (!filters.isEmpty()) b.filter(filters);
            if (customCriteria != null) customCriteria.accept(b); 
            return b;
        }));
    }

    private static Query buildTextQuery(String searchTerm, List<String> fields, String defaultField, String fuzziness) {
        if (!StringUtils.hasText(searchTerm)) return Query.of(q -> q.matchAll(m -> m));

        String fuzzyValue = StringUtils.hasText(fuzziness) ? fuzziness : "0";
        List<String> searchFields = CollectionUtils.isEmpty(fields) ? List.of(defaultField) : fields;

        return Query.of(q -> q.multiMatch(mm -> mm
                .fields(searchFields)
                .query(searchTerm)
                .fuzziness(fuzzyValue)
        ));
    }

    private static List<Query> buildTermFilters(Map<String, String> termFilters) {
        List<Query> queries = new ArrayList<>();
        if (termFilters == null) return queries;
        termFilters.forEach((field, value) -> {
            if (StringUtils.hasText(value)) queries.add(Query.of(q -> q.term(t -> t.field(field).value(value))));
        });
        return queries;
    }

    private static List<Query> buildRangeFilters(Map<String, RangeFilter> rangeFilters) {
        List<Query> queries = new ArrayList<>();
        if (rangeFilters == null) return queries;
        rangeFilters.forEach((field, range) -> {
            if (range == null) return;
            queries.add(Query.of(q -> q.range(r -> r.untyped(u -> {
                u.field(field);
                if (StringUtils.hasText(range.getFrom())) u.gte(JsonData.of(range.getFrom()));
                if (StringUtils.hasText(range.getTo())) u.lte(JsonData.of(range.getTo()));
                return u;
            }))));
        });
        return queries;
    }

    private static HighlightQuery buildHighlightQuery(List<String> fields, String defaultField) {
        List<String> hFields = CollectionUtils.isEmpty(fields) ? List.of(defaultField) : fields;
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<mark>").withPostTags("</mark>").build();
        return new HighlightQuery(new Highlight(params, hFields.stream().map(HighlightField::new).toList()), null);
    }
}
