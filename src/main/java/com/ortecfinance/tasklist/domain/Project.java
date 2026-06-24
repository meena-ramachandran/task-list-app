package com.ortecfinance.tasklist.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }

    public Optional<Task> findTask(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return tasks.stream().filter(t -> id.equals(t.getId())).findFirst();
    }

    public boolean removeTask(Long id) {
        if (id == null) {
            return false;
        }
        return tasks.removeIf(t -> id.equals(t.getId()));
    }
}
