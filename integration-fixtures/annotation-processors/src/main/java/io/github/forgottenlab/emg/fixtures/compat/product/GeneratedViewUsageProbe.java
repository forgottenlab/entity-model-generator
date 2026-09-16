package io.github.forgottenlab.emg.fixtures.compat.product;

import io.github.forgottenlab.emg.fixtures.compat.product.model.view.ProductAiView;

public final class GeneratedViewUsageProbe {

    private GeneratedViewUsageProbe() {
    }

    public static String roundTrip(ProductAiView view) {
        view.setName("sample");
        return view.getName();
    }
}
