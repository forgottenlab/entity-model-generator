package io.github.forgottenlab.emg.fixtures.compat.product.mapper;

import io.github.forgottenlab.emg.fixtures.compat.product.entity.ProductEntity;
import io.github.forgottenlab.emg.fixtures.compat.product.model.view.ProductAiView;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    ProductAiView toAiView(ProductEntity source);
}
