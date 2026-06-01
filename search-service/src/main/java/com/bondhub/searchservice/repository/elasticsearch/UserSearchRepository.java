<<<<<<<< HEAD:search-service/src/main/java/com/bondhub/searchservice/repository/elastic/UserSearchRepository.java
package com.bondhub.searchservice.repository.elastic;
========
package com.bondhub.searchservice.repository.elasticsearch;
>>>>>>>> eb452c700dc1e59691170534b60244dc464d868e:search-service/src/main/java/com/bondhub/searchservice/repository/elasticsearch/UserSearchRepository.java

import com.bondhub.searchservice.model.elasticsearch.UserIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserIndex, String> {
}
