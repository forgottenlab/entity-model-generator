package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.model.SourceMetadata;
import io.github.forgottenlab.emg.processor.support.PackageResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * 实体元数据解析器。
 *
 * <p>负责把带有 {@link AutoModel} 的实体类解析成统一的
 * {@link AutoModelMetadata} 对象，供后续校验与代码生成使用。</p>
 */
public class AutoModelMetadataResolver {

    public AutoModelMetadataResolver(ProcessingEnvironment ignored) {
        // 保留统一的 ProcessingEnvironment 构造入口。
    }

    /**
     * 解析单个实体类的完整元数据。
     *
     * @param element 带有 {@code @AutoModel} 的实体类元素
     * @return 统一元数据对象
     */
    public AutoModelMetadata resolve(SourceMetadata sourceMetadata, AutoModel autoModel) {
        AutoModelMetadata metadata = new AutoModelMetadata();
        metadata.setEntityPackageName(sourceMetadata.getPackageName());
        metadata.setEntityClassName(sourceMetadata.getClassName());
        metadata.setModelPrefix(autoModel.value());
        metadata.setGenerateDto(autoModel.generateDto());
        metadata.setGenerateBaseResponse(autoModel.generateBaseResponse());
        metadata.setGenerateListResponse(autoModel.generateListResponse());
        metadata.setGenerateConverter(autoModel.generateConverter());

        metadata.setDtoPackageName(PackageResolver.resolveDtoPackage(metadata.getEntityPackageName()));
        metadata.setResponsePackageName(PackageResolver.resolveResponsePackage(metadata.getEntityPackageName()));
        metadata.setConverterPackageName(PackageResolver.resolveConverterPackage(metadata.getEntityPackageName()));

        metadata.getFields().addAll(sourceMetadata.getFields());

        return metadata;
    }
}
