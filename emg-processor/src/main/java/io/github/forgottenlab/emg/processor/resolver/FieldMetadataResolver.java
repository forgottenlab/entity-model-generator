package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.annotations.DtoIgnore;
import io.github.forgottenlab.emg.annotations.ListIgnore;
import io.github.forgottenlab.emg.annotations.ResponseAlias;
import io.github.forgottenlab.emg.annotations.ResponseIgnore;
import io.github.forgottenlab.emg.annotations.ViewGroups;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.util.NameUtils;
import io.github.forgottenlab.emg.processor.support.ProcessorException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * 字段元数据解析器。
 *
 * <p>负责将单个实体字段上的注解配置解析为 {@link FieldMetadata}，
 * 供 DTO / Response / Converter 生成器统一使用。</p>
 */
public class FieldMetadataResolver {

    private final TypeMirrorTypeResolver typeResolver = new TypeMirrorTypeResolver();

    private final Types types;

    public FieldMetadataResolver(ProcessingEnvironment processingEnvironment) {
        this.types = processingEnvironment.getTypeUtils();
    }

    /**
     * 解析单个字段。
     *
     * @param element 字段元素
     * @return 字段元数据；若不是字段则返回 null
     */
    public FieldMetadata resolve(Element element,
                                 TypeElement sourceType,
                                 AutoModel autoModel) {
        if (element.getKind() != ElementKind.FIELD) {
            return null;
        }

        VariableElement field = (VariableElement) element;
        FieldMetadata metadata = new FieldMetadata();
        String fieldName = field.getSimpleName().toString();
        TypeMirrorTypeResolver.ResolvedType resolvedType;
        try {
            resolvedType = typeResolver.resolve(field.asType());
        } catch (IllegalArgumentException exception) {
            throw new ProcessorException(exception.getMessage(), field, exception);
        }

        metadata.setSourceFieldName(fieldName);
        metadata.setResponseFieldName(resolveResponseFieldName(field, fieldName));
        metadata.setQualifiedTypeName(resolvedType.qualifiedName());
        metadata.setSimpleTypeName(resolvedType.simpleName());
        metadata.getReferencedTypeNames().addAll(resolvedType.referencedTypeNames());
        resolveViewGroups(field, metadata);

        // DTO 生成规则：只要没有 @DtoIgnore，就允许进入 DTO
        metadata.setGenerateForDto(field.getAnnotation(DtoIgnore.class) == null);

        // Response 生成规则：
        // 1. @ResponseIgnore 会屏蔽所有 Response
        // 2. @ListIgnore 只会屏蔽 ListResponse
        boolean responseIgnore = field.getAnnotation(ResponseIgnore.class) != null;
        metadata.setGenerateForBaseResponse(!responseIgnore);
        metadata.setGenerateForListResponse(!responseIgnore && field.getAnnotation(ListIgnore.class) == null);

        if (requiresGetter(metadata, autoModel)) {
            metadata.setSourceGetterName(resolveGetter(field, sourceType));
        }

        return metadata;
    }

    private boolean requiresGetter(FieldMetadata metadata, AutoModel autoModel) {
        return autoModel != null
                && autoModel.generateConverter()
                && ((autoModel.generateDto() && metadata.isGenerateForDto())
                || (autoModel.generateBaseResponse() && metadata.isGenerateForBaseResponse())
                || (autoModel.generateListResponse() && metadata.isGenerateForListResponse()));
    }

    private void resolveViewGroups(VariableElement field, FieldMetadata metadata) {
        ViewGroups annotation = field.getAnnotation(ViewGroups.class);
        if (annotation == null) {
            return;
        }
        for (String group : annotation.value()) {
            if (!metadata.getViewGroups().contains(group)) {
                metadata.getViewGroups().add(group);
            }
        }
    }

    private String resolveGetter(VariableElement field, TypeElement sourceType) {
        String suffix = NameUtils.capitalize(field.getSimpleName().toString());
        List<String> candidateNames = field.asType().getKind() == TypeKind.BOOLEAN
                ? List.of("is" + suffix, "get" + suffix)
                : List.of("get" + suffix);

        for (String candidateName : candidateNames) {
            for (Element enclosed : sourceType.getEnclosedElements()) {
                if (isCallableGetter(enclosed, candidateName, field)) {
                    return candidateName;
                }
            }
        }

        String expected = field.asType().getKind() == TypeKind.BOOLEAN
                ? String.join(" 或 ", candidateNames.stream().map(name -> name + "()").toList())
                : candidateNames.get(0) + "()";
        throw new ProcessorException(
                "Converter 无法读取字段 " + field.getSimpleName()
                        + "：需要 public 实例零参 getter " + expected
                        + "，且返回类型为 " + field.asType(),
                field
        );
    }

    private boolean isCallableGetter(Element element,
                                     String expectedName,
                                     VariableElement field) {
        if (element.getKind() != ElementKind.METHOD
                || !element.getSimpleName().contentEquals(expectedName)
                || !element.getModifiers().contains(Modifier.PUBLIC)
                || element.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }

        ExecutableElement method = (ExecutableElement) element;
        return method.getParameters().isEmpty()
                && types.isSameType(method.getReturnType(), field.asType());
    }

    /**
     * 解析字段在 Response 中的最终字段名。
     *
     * <p>若存在 {@link ResponseAlias}，则使用别名；
     * 否则使用原字段名。</p>
     */
    private String resolveResponseFieldName(VariableElement field, String defaultName) {
        ResponseAlias alias = field.getAnnotation(ResponseAlias.class);
        return alias == null ? defaultName : alias.value();
    }
}
