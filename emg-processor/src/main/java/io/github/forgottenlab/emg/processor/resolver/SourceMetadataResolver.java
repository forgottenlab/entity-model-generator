package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.model.SourceMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * 将一个来源类型及其直接声明字段解析一次并形成共享元数据。
 */
public class SourceMetadataResolver {

    private final ProcessingEnvironment processingEnvironment;

    private final FieldMetadataResolver fieldMetadataResolver;

    public SourceMetadataResolver(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.fieldMetadataResolver = new FieldMetadataResolver(processingEnvironment);
    }

    public SourceMetadata resolve(TypeElement typeElement, AutoModel autoModel) {
        PackageElement packageElement = processingEnvironment.getElementUtils().getPackageOf(typeElement);
        SourceMetadata metadata = new SourceMetadata();
        metadata.setPackageName(packageElement.getQualifiedName().toString());
        metadata.setClassName(typeElement.getSimpleName().toString());

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD
                    || enclosed.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            FieldMetadata field = fieldMetadataResolver.resolve(enclosed, typeElement, autoModel);
            if (field != null) {
                metadata.getFields().add(field);
            }
        }
        return metadata;
    }
}
