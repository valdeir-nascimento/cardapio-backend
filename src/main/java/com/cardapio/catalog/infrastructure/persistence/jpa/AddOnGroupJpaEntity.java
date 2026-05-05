package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "addon_groups")
public class AddOnGroupJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "min_selection", nullable = false) private int minSelection;
    @Column(name = "max_selection", nullable = false) private int maxSelection;
    @Column(nullable = false) private int position;

    @OneToMany(mappedBy = "groupId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Fetch(FetchMode.SUBSELECT)
    private List<AddOnItemJpaEntity> items = new ArrayList<>();

    protected AddOnGroupJpaEntity() {}

    public AddOnGroupJpaEntity(UUID id, UUID productId, String name, int minSelection, int maxSelection, int position) {
        this.id = id; this.productId = productId; this.name = name;
        this.minSelection = minSelection; this.maxSelection = maxSelection; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getName() { return name; }
    public int getMinSelection() { return minSelection; }
    public int getMaxSelection() { return maxSelection; }
    public int getPosition() { return position; }
    public List<AddOnItemJpaEntity> getItems() { return items; }
}
