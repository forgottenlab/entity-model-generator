package io.github.forgottenlab.emg.fixtures.compat.product;

import io.github.forgottenlab.emg.fixtures.compat.product.entity.ProductEntity;

public final class LombokUsageProbe {

    private LombokUsageProbe() {
    }

    public static String roundTrip(ProductEntity product) {
        product.setName("sample");
        return product.getName();
    }
}
