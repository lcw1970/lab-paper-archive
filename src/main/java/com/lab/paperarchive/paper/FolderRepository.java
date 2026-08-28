package com.lab.paperarchive.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByOrderByNameAsc();

    Optional<Folder> findByNameIgnoreCase(String name);
}
