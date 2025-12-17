package com.scholarsync.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
