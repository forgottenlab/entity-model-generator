package io.github.forgottenlab.emg.fixtures.compat.product.entity;

import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.annotations.ViewGroups;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AutoView(value = "ai", name = "ProductAiView")
public class ProductEntity {

    @ViewGroups("ai")
    private Long id;

    @ViewGroups("ai")
    private String name;

    @ViewGroups("ai")
    private String description;

    @ViewGroups("ai")
    private BigDecimal price;

    @ViewGroups("ai")
    private Integer stock;

    @ViewGroups("ai")
    private Long categoryId;
}
