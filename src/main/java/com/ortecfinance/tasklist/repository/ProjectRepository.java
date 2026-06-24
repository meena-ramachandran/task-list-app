package com.ortecfinance.tasklist.repository;

import com.ortecfinance.tasklist.domain.Project;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);
    boolean existsByName(String name);

    @Transactional
    @Modifying
    void deleteByName(String name);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.tasks")
    List<Project> findAllWithTasks();
}
