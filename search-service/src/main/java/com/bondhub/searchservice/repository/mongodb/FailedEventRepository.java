<<<<<<<< HEAD:search-service/src/main/java/com/bondhub/searchservice/repository/mongo/FailedEventRepository.java
package com.bondhub.searchservice.repository.mongo;
========
package com.bondhub.searchservice.repository.mongodb;
>>>>>>>> eb452c700dc1e59691170534b60244dc464d868e:search-service/src/main/java/com/bondhub/searchservice/repository/mongodb/FailedEventRepository.java

import com.bondhub.common.model.kafka.EventType;
import com.bondhub.searchservice.model.mongodb.FailedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FailedEventRepository extends MongoRepository<FailedEvent, String> {

    long countByResolved(boolean resolved);

    long countByResolvedAndTopicIn(boolean resolved, Collection<String> topics);

    List<FailedEvent> findAllByResolved(boolean resolved);

    List<FailedEvent> findAllByResolvedAndTopicIn(boolean resolved, Collection<String> topics);

    List<FailedEvent> findAllByResolvedAndCreatedAtAfter(boolean resolved, LocalDateTime createdAt);

    Optional<FailedEvent> findFirstByEventIdAndEventTypeOrderByCreatedAtDesc(String eventId, EventType eventType);

    Optional<FailedEvent> findFirstByEventIdAndEventTypeAndResolvedFalseOrderByCreatedAtDesc(String eventId, EventType eventType);
}
