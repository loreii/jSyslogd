package it.loreii.AppSyslogd;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DictionaryRepository extends MongoRepository<Dictionary, String> {

    Page<Dictionary> findByValueLike(String value, Pageable pageable);

	List<Dictionary> findByValue(String hostname);
	List<Dictionary> findByType(String type);
}
