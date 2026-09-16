package io.github.forgottenlab.emg.fixtures.plain.product;

import io.github.forgottenlab.emg.fixtures.plain.product.model.view.ProductBasicView;

public final class PlainViewUsageProbe {

    private PlainViewUsageProbe() {
    }

    public static String roundTrip(ProductBasicView view) {
        view.setName("sample");
        return view.getName();
    }
}
