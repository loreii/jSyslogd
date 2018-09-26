package it.loreii.AppSyslogd;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.loreii.AppSyslogd.SyslogMsg.Data;

public interface SyslogDataRepository extends MongoRepository<SyslogMsg.Data, String> {

    //public SyslogMsg findBySubmission(Long fromUTC,Long toUTC);
    Page<SyslogMsg.Data> findByHostnameLike(String hostname, Pageable pageable);
  
    List<SyslogMsg.Data> findByTimestampBetween(Date dateGT, Date dateLT);
    List<SyslogMsg.Data> findByTimestampAfter(Date dateGT);
    List<SyslogMsg.Data> findByTimestampBefore(Date dateLt);
    
    /**
     * db.data.createIndex({hostname: "text",msg: "text",structuredData:"text"})
     * */
    @Query("{ $text: { $search: ?0 } }")
    Page<SyslogMsg.Data> findByText(String text, Pageable pageable);

	Page<Data> findByHostname(String search, Pageable pageRequest);
    
}
