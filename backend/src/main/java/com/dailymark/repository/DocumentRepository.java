package com.dailymark.repository;

import com.dailymark.entity.Document;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findAllByOrderByUploadedAtDesc();
    Optional<Document> findByFilename(String filename);
    @Query("select d from Document d where lower(d.title) like lower(concat('%', :query, '%')) escape '\\' or lower(cast(d.content as string)) like lower(concat('%', :query, '%')) escape '\\' order by d.uploadedAt desc")
    List<Document> search(@Param("query") String query);
}
