package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.processor.support.PackageResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * 实体元数据解析器。
 *
 * <p>负责把带有 {@link AutoModel} 的实体类解析成统一的
 * {@link AutoModelMetadata} 对象，供后续校验与代码生成使用。</p>
 */
public class AutoModelMetadataResolver {

    /**
     * 字段元数据解析器。
     */
    private final FieldMetadataResolver fieldMetadataResolver;

    /**
     * APT 处理环境。
     */
    private final ProcessingEnvironment processingEnvironment;

    public AutoModelMetadataResolver(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.fieldMetadataResolver = new FieldMetadataResolver(processingEnvironment);
    }

    /**
     * 解析单个实体类的完整元数据。
     *
     * @param element 带有 {@code @AutoModel} 的实体类元素
     * @return 统一元数据对象
     */
    public AutoModelMetadata resolve(Element element) {
        TypeElement typeElement = (TypeElement) element;
        AutoModel autoModel = typeElement.getAnnotation(AutoModel.class);
        PackageElement packageElement = processingEnvironment.getElementUtils().getPackageOf(typeElement);

        AutoModelMetadata metadata = new AutoModelMetadata();
        metadata.setEntityPackageName(packageElement.getQualifiedName().toString());
        metadata.setEntityClassName(typeElement.getSimpleName().toString());
        metadata.setModelPrefix(autoModel.value());
        metadata.setGenerateDto(autoModel.generateDto());
        metadata.setGenerateBaseResponse(autoModel.generateBaseResponse());
        metadata.setGenerateListResponse(autoModel.generateListResponse());
        metadata.setGenerateConverter(autoModel.generateConverter());

        metadata.setDtoPackageName(PackageResolver.resolveDtoPackage(metadata.getEntityPackageName()));
        metadata.setResponsePackageName(PackageResolver.resolveResponsePackage(metadata.getEntityPackageName()));
        metadata.setConverterPackageName(PackageResolver.resolveConverterPackage(metadata.getEntityPackageName()));

        for (Element enclosed : typeElement.getEnclosedElements()) {
            // V1 只复制当前实体直接声明的实例字段；getEnclosedElements 不包含继承字段。
            if (enclosed.getKind() != ElementKind.FIELD
                    || enclosed.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            FieldMetadata fieldMetadata = fieldMetadataResolver.resolve(enclosed, typeElement, autoModel);
            if (fieldMetadata != null) {
                metadata.getFields().add(fieldMetadata);
            }
        }

        return metadata;
    }
}
