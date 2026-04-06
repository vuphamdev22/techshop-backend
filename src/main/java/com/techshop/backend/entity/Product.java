package com.techshop.backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double price;
    private Double originalPrice;
    private Integer stock;
    private Double rating;
    private Integer reviews;
    private String badge;
    @Column(name = "sku", unique = true)
    private String sku;
    @Column(name = "min_stock")
    private Integer minStock;
    @Column(name = "max_stock")
    private Integer maxStock;
    @Column(name = "last_restocked")
    private LocalDate lastRestocked;

    // 🔗 Category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // 🔗 Product Images (1 product - nhiều ảnh)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<ProductImage> images;

    // 🔗 Specs (key-value)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpec> specs;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.lastRestocked == null && this.stock != null && this.stock > 0) {
            this.lastRestocked = LocalDate.now();
        }
        createdAt = LocalDateTime.now();
    }
}
