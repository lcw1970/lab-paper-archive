package com.lab.paperarchive.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    @Query(value = """
           select a from AuditLog a
           where :qNfc = ''
              or lower(a.actorEmail) like lower(concat('%', :qNfc, '%'))
              or lower(a.actorEmail) like lower(concat('%', :qNfd, '%'))
              or lower(a.targetType) like lower(concat('%', :qNfc, '%'))
              or lower(a.targetType) like lower(concat('%', :qNfd, '%'))
              or lower(a.description) like lower(concat('%', :qNfc, '%'))
              or lower(a.description) like lower(concat('%', :qNfd, '%'))
           order by a.occurredAt desc
           """,
            countQuery = """
           select count(a) from AuditLog a
           where :qNfc = ''
              or lower(a.actorEmail) like lower(concat('%', :qNfc, '%'))
              or lower(a.actorEmail) like lower(concat('%', :qNfd, '%'))
              or lower(a.targetType) like lower(concat('%', :qNfc, '%'))
              or lower(a.targetType) like lower(concat('%', :qNfd, '%'))
              or lower(a.description) like lower(concat('%', :qNfc, '%'))
              or lower(a.description) like lower(concat('%', :qNfd, '%'))
           """)
    Page<AuditLog> search(@Param("qNfc") String queryNfc,
                          @Param("qNfd") String queryNfd,
                          Pageable pageable);
}
