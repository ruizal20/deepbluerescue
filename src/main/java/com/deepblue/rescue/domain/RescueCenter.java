package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rescue_centers")
public class RescueCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Corregido a Long

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name; // Cambiado de rname a name para coincidir con la BD

    @Column(nullable = false)
    private String city;

    @OneToMany(mappedBy = "rescueCenter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RescueCase> cases = new ArrayList<>();

    public void addCase(RescueCase rescueCase) {
        cases.add(rescueCase);
        rescueCase.setRescueCenter(this);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<RescueCase> getCases() {
        return cases;
    }
}