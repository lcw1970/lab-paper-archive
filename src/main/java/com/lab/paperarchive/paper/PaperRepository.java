package com.lab.paperarchive.paper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    Optional<Paper> findByIdAndDeletedAtIsNull(Long id);

    Page<Paper> findByDeletedAtIsNull(Pageable pageable);

    Page<Paper> findByDeletedAtIsNotNull(Pageable pageable);

    long countByDeletedAtIsNull();

    long countByFolderIdAndDeletedAtIsNull(Long folderId);

    long countByFolderIsNullAndDeletedAtIsNull();

    List<Paper> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    List<Paper> findAllByIdInAndDeletedAtIsNotNull(List<Long> ids);

    @Query("""
           select distinct p from Paper p
           left join fetch p.files
           where p.id = :id and p.deletedAt is not null
           """)
    Optional<Paper> findDeletedWithFilesById(@Param("id") Long id);

    @Query("""
           select distinct p from Paper p
           left join fetch p.files
           where p.id in :ids and p.deletedAt is not null
           """)
    List<Paper> findAllDeletedWithFilesByIdIn(@Param("ids") List<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Paper p set p.folder = null where p.folder.id = :folderId")
    void clearFolder(@Param("folderId") Long folderId);

    @Query("""
           select p from Paper p
           left join fetch p.uploader
           left join fetch p.folder
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
           left join fetch p.folder
           left join p.tags t
           where p.deletedAt is null
             and (:kw = ''
                  or lower(p.title)   like lower(concat('%', :kw, '%'))
                  or lower(p.authors) like lower(concat('%', :kw, '%'))
                  or lower(p.memo)    like lower(concat('%', :kw, '%')))
             and (:tag = '' or lower(t.name) = lower(:tag))
             and (:folderId is null or p.folder.id = :folderId)
             and (:uncategorized = false or p.folder is null)
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
             and (:folderId is null or p.folder.id = :folderId)
             and (:uncategorized = false or p.folder is null)
           """)
    Page<Paper> search(@Param("kw") String keyword,
                       @Param("tag") String tag,
                       @Param("folderId") Long folderId,
                       @Param("uncategorized") boolean uncategorized,
                       Pageable pageable);

    @Query("""
           select distinct p from Paper p
           left join fetch p.files
           where p.id in :ids and p.deletedAt is null
           """)
    List<Paper> findAllForDownload(@Param("ids") List<Long> ids);
}
