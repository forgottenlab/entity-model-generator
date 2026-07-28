package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.DtoIgnore;
import io.github.forgottenlab.emg.annotations.ListIgnore;
import io.github.forgottenlab.emg.annotations.ResponseAlias;
import io.github.forgottenlab.emg.annotations.ResponseIgnore;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.util.TypeUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * 字段元数据解析器。
 *
 * <p>负责将单个实体字段上的注解配置解析为 {@link FieldMetadata}，
 * 供 DTO / Response / Converter 生成器统一使用。</p>
 */
public class FieldMetadataResolver {

    /**
     * 解析单个字段。
     *
     * @param element 字段元素
     * @return 字段元数据；若不是字段则返回 null
     */
    public FieldMetadata resolve(Element element) {
        if (element.getKind() != ElementKind.FIELD) {
            return null;
        }

        VariableElement field = (VariableElement) element;
        FieldMetadata metadata = new FieldMetadata();
        String fieldName = field.getSimpleName().toString();
        String qualifiedTypeName = field.asType().toString();

        metadata.setSourceFieldName(fieldName);
        metadata.setResponseFieldName(resolveResponseFieldName(field, fieldName));
        metadata.setQualifiedTypeName(qualifiedTypeName);
        metadata.setSimpleTypeName(TypeUtils.simpleName(qualifiedTypeName));

        // DTO 生成规则：只要没有 @DtoIgnore，就允许进入 DTO
        metadata.setGenerateForDto(field.getAnnotation(DtoIgnore.class) == null);

        // Response 生成规则：
        // 1. @ResponseIgnore 会屏蔽所有 Response
        // 2. @ListIgnore 只会屏蔽 ListResponse
        boolean responseIgnore = field.getAnnotation(ResponseIgnore.class) != null;
        metadata.setGenerateForBaseResponse(!responseIgnore);
        metadata.setGenerateForListResponse(!responseIgnore && field.getAnnotation(ListIgnore.class) == null);

        return metadata;
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