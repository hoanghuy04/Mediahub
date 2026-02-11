package com.bondhub.userservice.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.IndicesStatsResponse;
import co.elastic.clients.elasticsearch.indices.stats.IndexStats;
import co.elastic.clients.elasticsearch.core.CountResponse;
import com.bondhub.common.dto.ApiResponse;
import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.common.utils.LocalizationUtil;
import com.bondhub.userservice.client.AuthServiceClient;
import com.bondhub.userservice.config.ElasticsearchProperties;
import com.bondhub.userservice.dto.response.AccountResponse;
import com.bondhub.userservice.dto.response.elasticsearch.*;
import com.bondhub.userservice.enums.DataSyncStatus;
import com.bondhub.userservice.enums.ElasticsearchClusterStatus;
import com.bondhub.userservice.enums.IndexStatus;
import com.bondhub.userservice.enums.ReindexTaskStatus;
import com.bondhub.userservice.model.User;
import com.bondhub.userservice.model.elasticsearch.UserIndex;
import com.bondhub.userservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElasticsearchAdminServiceImpl implements ElasticsearchAdminService {

    ElasticsearchOperations esOps;
    ElasticsearchClient esClient;
    UserRepository userRepository;
    ElasticsearchProperties esProperties;
    AuthServiceClient authServiceClient;
    LocalizationUtil localizationUtil;
    ReindexTaskTracker reindexTaskTracker;
    UserDeadEventService userDeadEventService;

    @Override
    public ElasticsearchSummaryResponse getSummary() {
        return ElasticsearchSummaryResponse.builder()
                .health(getHealth())
                .stats(getIndexStats())
                .compare(compareWithDatabase())
                .deadEventsCount(userDeadEventService.countDeadEvents())
                .build();
    }

    @Override
    public ElasticsearchHealthResponse getHealth() {
        try {
            HealthResponse healthResponse = esClient.cluster().health();
            String alias = esProperties.getUserAlias();
            String currentIndexName = getActualIndexName(alias);
            boolean indexExists = !currentIndexName.equals(alias);

            return ElasticsearchHealthResponse.builder()
                    .status(convertHealthStatus(healthResponse.status()))
                    .clusterName(healthResponse.clusterName())
                    .indexExists(indexExists)
                    .currentIndexName(indexExists ? currentIndexName : null)
                    .aliasName(alias)
                    .build();

        } catch (IOException e) {
            log.error("Failed to get Elasticsearch health: {}", e.getMessage(), e);
            return ElasticsearchHealthResponse.builder()
                    .status(ElasticsearchClusterStatus.UNREACHABLE)
                    .clusterName("unknown")
                    .indexExists(false)
                    .currentIndexName(null)
                    .aliasName(esProperties.getUserAlias())
                    .build();
        }
    }

    @Override
    public IndexStatsResponse getIndexStats() {
        try {
            String alias = esProperties.getUserAlias();
            String actualIndex = getActualIndexName(alias);

            var exists = esClient.indices().exists(e -> e.index(actualIndex));
            if (!exists.value()) {
                return IndexStatsResponse.builder()
                        .indexName(alias)
                        .documentCount(0L)
                        .primaryStoreSize("0b")
                        .totalStoreSize("0b")
                        .numberOfShards(0)
                        .numberOfReplicas(0)
                        .build();
            }

            IndicesStatsResponse statsResponse = esClient.indices().stats(s -> s.index(actualIndex));

            var indicesStatsMap = statsResponse.indices();
            var indexStats = indicesStatsMap.get(actualIndex);

            if (indexStats == null && !indicesStatsMap.isEmpty()) {
                indexStats = indicesStatsMap.values().iterator().next();
            }

            if (indexStats == null) {
                return IndexStatsResponse.builder()
                        .indexName(actualIndex)
                        .documentCount(0L)
                        .primaryStoreSize("0b")
                        .totalStoreSize("0b")
                        .numberOfShards(0)
                        .numberOfReplicas(0)
                        .build();
            }

            var primaries = indexStats.primaries();
            var total = indexStats.total();

            return IndexStatsResponse.builder()
                    .indexName(actualIndex)
                    .documentCount(primaries != null && primaries.docs() != null ? primaries.docs().count() : 0)
                    .primaryStoreSize(formatStoreSize(primaries))
                    .totalStoreSize(formatStoreSize(total))
                    .numberOfShards(statsResponse.shards() != null ? statsResponse.shards().total().intValue() : 0)
                    .numberOfReplicas(0)
                    .build();

        } catch (IOException e) {
            log.error("Failed to get index stats: {}", e.getMessage(), e);
            return IndexStatsResponse.builder()
                    .indexName("unavailable")
                    .documentCount(0L)
                    .primaryStoreSize("0b")
                    .totalStoreSize("0b")
                    .numberOfShards(0)
                    .numberOfReplicas(0)
                    .build();
        }
    }

    private String formatStoreSize(IndexStats stats) {
        if (stats == null || stats.store() == null) {
            return "0b";
        }
        
        String size = stats.store().size();
        if (size != null && !size.isEmpty()) {
            return size;
        }

        Long bytes = stats.store().sizeInBytes();
        return formatByteSize(bytes);
    }

    private String formatByteSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0b";
        final String[] units = new String[] { "b", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + units[digitGroups];
    }

    @Override
    public DataComparisonResponse compareWithDatabase() {
        try {
            if (reindexTaskTracker.isReindexRunning()) {
                return DataComparisonResponse.builder()
                        .elasticsearchCount(0)
                        .databaseCount(0)
                        .difference(0)
                        .status(DataSyncStatus.IN_SYNC)
                        .recommendation(localizationUtil.getMessage("search.compare.reindexing"))
                        .build();
            }

            long esCount = getElasticsearchDocumentCount();
            long dbCount = userRepository.count();
            long difference = Math.abs(esCount - dbCount);

            DataSyncStatus status;
            String recommendation;

            if (difference == 0) {
                status = DataSyncStatus.IN_SYNC;
                recommendation = localizationUtil.getMessage("search.compare.recommendation.in_sync");
            } else if (esCount > dbCount) {
                status = DataSyncStatus.ES_AHEAD;
                recommendation = localizationUtil.getMessage("search.compare.recommendation.es_ahead", difference);
            } else {
                status = DataSyncStatus.DB_AHEAD;
                recommendation = localizationUtil.getMessage("search.compare.recommendation.db_ahead", difference);
            }

            return DataComparisonResponse.builder()
                    .elasticsearchCount(esCount)
                    .databaseCount(dbCount)
                    .difference(difference)
                    .status(status)
                    .recommendation(recommendation)
                    .build();
        } catch (Exception e) {
            log.error("Failed to compare data: {}", e.getMessage(), e);
            return DataComparisonResponse.builder()
                    .elasticsearchCount(0)
                    .databaseCount(0)
                    .difference(0)
                    .status(DataSyncStatus.IN_SYNC)
                    .recommendation(localizationUtil.getMessage("search.compare.unavailable"))
                    .build();
        }
    }

    @Override
    public UserIndex getDocument(String userId) {
        UserIndex userIndex = esOps.get(userId, UserIndex.class, IndexCoordinates.of(esProperties.getUserAlias()));
        if (userIndex == null) {
            throw new AppException(ErrorCode.EL_DOCUMENT_NOT_FOUND);
        }
        return userIndex;
    }

    @Override
    public void reindexUser(String userId) {
        if (reindexTaskTracker.isReindexRunning()) {
            throw new AppException(ErrorCode.EL_REINDEX_IN_PROGRESS);
        }

        ElasticsearchHealthResponse health = getHealth();
        if (health.status() == ElasticsearchClusterStatus.RED
                || health.status() == ElasticsearchClusterStatus.UNREACHABLE) {
            throw new AppException(ErrorCode.EL_CLUSTER_UNHEALTHY);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        AccountResponse account = fetchAccount(user.getAccountId());

        UserIndex userIndex = UserIndex.builder()
                .id(user.getId())
                .accountId(user.getAccountId())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .phoneNumber(account != null ? account.phoneNumber() : null)
                .role(account != null ? account.role() : null)
                .createdAt(LocalDateTime.now())
                .build();

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(userIndex.getId())
                .withObject(userIndex)
                .build();

        esOps.index(indexQuery, IndexCoordinates.of(esProperties.getUserAlias()));
        log.info("Successfully reindexed user: {}", userId);
    }

    @Override
    public List<IndexDetailResponse> getAllUserIndexes() {
        try {
            String alias = esProperties.getUserAlias();
            String pattern = alias + "*";

            GetAliasResponse aliasResponse = esClient.indices().getAlias(g -> g.index(pattern));
            List<String> indexNames = new ArrayList<>(aliasResponse.result().keySet());

            if (indexNames.isEmpty()) return List.of();

            IndicesStatsResponse allStats = esClient.indices().stats(s -> s.index(indexNames));
            String currentIndex = getActualIndexName(alias);

            return indexNames.stream()
                    .map(name -> {
                        var stats = allStats.indices().get(name);
                        long docCount = (stats != null && stats.primaries() != null && stats.primaries().docs() != null)
                                ? stats.primaries().docs().count() : 0;

                        return new IndexDetailResponse(
                                name,
                                parseTimestampFromIndexName(name),
                                docCount,
                                formatStoreSize(stats != null ? stats.primaries() : null),
                                name.equals(currentIndex) ? IndexStatus.ACTIVE : IndexStatus.STANDBY
                        );
                    })
                    .sorted(Comparator.comparing(IndexDetailResponse::createdAt).reversed())
                    .toList();
        } catch (IOException e) {
            log.error("Failed to get detailed user indexes: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public IndexOperationResponse switchAlias(String targetIndexName) {
        try {
            String alias = esProperties.getUserAlias();
            String oldIndex = getActualIndexName(alias);

            var exists = esClient.indices().exists(e -> e.index(targetIndexName));
            if (!exists.value()) throw new AppException(ErrorCode.EL_INDEX_NOT_FOUND);

            esClient.indices().updateAliases(u -> u
                    .actions(a -> a.remove(r -> r.index(oldIndex).alias(alias)))
                    .actions(a -> a.add(ad -> ad.index(targetIndexName).alias(alias)))
            );

            log.info("Rollback: Switched alias {} from {} to {}", alias, oldIndex, targetIndexName);
            return new IndexOperationResponse(localizationUtil.getMessage("search.alias.switch.success"), targetIndexName);
        } catch (IOException e) {
            throw new AppException(ErrorCode.EL_CLUSTER_UNHEALTHY);
        }
    }

    @Override
    public IndexOperationResponse deletePhysicalIndex(String indexName) {
        try {
            String currentIndex = getActualIndexName(esProperties.getUserAlias());

            if (indexName.equals(currentIndex)) {
                throw new AppException(ErrorCode.EL_CLUSTER_UNHEALTHY);
            }

            esClient.indices().delete(d -> d.index(indexName));
            log.info("Admin deleted physical index: {}", indexName);

            return new IndexOperationResponse(localizationUtil.getMessage("search.index.delete.success"), indexName);
        } catch (IOException e) {
            throw new AppException(ErrorCode.EL_CLUSTER_UNHEALTHY);
        }
    }

    private LocalDateTime parseTimestampFromIndexName(String indexName) {
        try {
            String[] parts = indexName.split("_");
            long timestamp = Long.parseLong(parts[parts.length - 1]);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String getActualIndexName(String alias) {
        try {
            GetAliasResponse aliasResponse = esClient.indices().getAlias(g -> g.name(alias));
            var result = aliasResponse.result().keySet().stream()
                    .filter(index -> !index.startsWith("."))
                    .sorted(Comparator.reverseOrder())
                    .findFirst();

            if (result.isEmpty()) {
                log.warn("Alias '{}' exists but resolved to no physical index. Downstream operations may use alias name as fallback — data may be stale.", alias);
                return alias;
            }
            return result.get();
        } catch (Exception e) {
            log.warn("Failed to resolve actual index for alias '{}': {}. Using alias as fallback — data may be stale.", alias, e.getMessage());
            return alias;
        }
    }

    private long getElasticsearchDocumentCount() {
        try {
            String alias = esProperties.getUserAlias();
            CountResponse countResponse = esClient.count(c -> c.index(alias));
            return countResponse.count();
        } catch (IOException e) {
            log.error("Failed to get ES document count: {}", e.getMessage());
            return 0;
        }
    }

    private ElasticsearchClusterStatus convertHealthStatus(HealthStatus status) {
        if (status == null) return ElasticsearchClusterStatus.UNKNOWN;
        return switch (status) {
            case Green -> ElasticsearchClusterStatus.GREEN;
            case Yellow -> ElasticsearchClusterStatus.YELLOW;
            case Red -> ElasticsearchClusterStatus.RED;
            default -> ElasticsearchClusterStatus.UNKNOWN;
        };
    }

    private AccountResponse fetchAccount(String accountId) {
        try {
            ApiResponse<List<AccountResponse>> response = authServiceClient.getAccountsByIds(List.of(accountId));
            if (response != null && response.data() != null && !response.data().isEmpty()) {
                return response.data().getFirst();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch account {}: {}", accountId, e.getMessage());
        }
        return null;
    }
}
