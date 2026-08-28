package com.lab.paperarchive.paper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    Optional<Paper> findByIdAndDeletedAtIsNull(Long id);

    Page<Paper> findByDeletedAtIsNull(Pageable pageable);

    long countByDeletedAtIsNull();

    @Query("""
           select p from Paper p
           left join fetch p.uploader
           left join fetch p.tags
           where p.id = :id and p.deletedAt is null
           """)
    Optional<Paper> findDetailWithTags(@Param("id") Long id);

    @Query("""
           select p from Paper p
           left join fetch p.files
           where p.id = :id and p.deletedAt is null
           """)
    Optional<Paper> findDetailWithFiles(@Param("id") Long id);

    @Query(value = """
           select distinct p from Paper p
           left join fetch p.uploader
           left join p.tags t
           where p.deletedAt is null
             and (:kw = ''
                  or lower(p.title)   like lower(concat('%', :kw, '%'))
                  or lower(p.authors) like lower(concat('%', :kw, '%'))
                  or lower(p.memo)    like lower(concat('%', :kw, '%')))
             and (:tag = '' or lower(t.name) = lower(:tag))
           """,
            countQuery = """
           select count(distinct p) from Paper p
           left join p.tags t
           where p.deletedAt is null
             and (:kw = ''
                  or lower(p.title)   like lower(concat('%', :kw, '%'))
                  or lower(p.authors) like lower(concat('%', :kw, '%'))
                  or lower(p.memo)    like lower(concat('%', :kw, '%')))
             and (:tag = '' or lower(t.name) = lower(:tag))
           """)
    Page<Paper> search(@Param("kw") String keyword,
                       @Param("tag") String tag,
                       Pageable pageable);

    @Query("""
           select distinct p from Paper p
           left join fetch p.files
           where p.id in :ids and p.deletedAt is null
           """)
    List<Paper> findAllForDownload(@Param("ids") List<Long> ids);
}
