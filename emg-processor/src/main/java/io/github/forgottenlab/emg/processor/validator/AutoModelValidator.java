package io.github.forgottenlab.emg.processor.validator;

import io.github.forgottenlab.emg.annotations.ResponseAlias;
import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.processor.support.ProcessorException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * EMG V1 编译期输入与生成目标校验器。
 */
public class AutoModelValidator {

    public AutoModelValidator(ProcessingEnvironment ignored) {
        // 保留 ProcessingEnvironment 构造入口，与其他 processor 组件初始化方式一致。
    }

    /**
     * 在元数据解析前校验注解目标，避免对非 class 元素做不安全转换。
     */
    public void validateAnnotatedElement(Element sourceElement) {
        if (sourceElement.getKind() != ElementKind.CLASS) {
            throw new ProcessorException(
                    "@AutoModel 只能标注在 class 上: " + sourceElement,
                    sourceElement
            );
        }
    }

    /**
     * 校验单个实体的命名与 Response 字段映射。
     */
    public void validate(AutoModelMetadata metadata, Element sourceElement) {
        validateJavaIdentifier("@AutoModel.value()", metadata.getModelPrefix(), sourceElement);
        validateJavaIdentifier("DTO 目标类名", metadata.getDtoClassName(), sourceElement);
        validateJavaIdentifier("BaseResponse 目标类名", metadata.getBaseResponseClassName(), sourceElement);
        validateJavaIdentifier("ListResponse 目标类名", metadata.getListResponseClassName(), sourceElement);
        validateJavaIdentifier("Converter 目标类名", metadata.getConverterClassName(), sourceElement);
        validateResponseAliases(sourceElement);
        validateResponseFieldConflicts(metadata, sourceElement);
    }

    /**
     * 在任何源码写入前校验本轮所有启用目标的全限定名唯一性。
     */
    public void validateTargetTypeConflicts(Map<AutoModelMetadata, Element> candidates) {
        Map<String, TargetOwner> owners = new LinkedHashMap<>();
        for (Map.Entry<AutoModelMetadata, Element> candidate : candidates.entrySet()) {
            AutoModelMetadata metadata = candidate.getKey();
            Element sourceElement = candidate.getValue();
            if (metadata.isGenerateDto()) {
                registerTarget(metadata.getDtoQualifiedName(), metadata, sourceElement, owners);
            }
            if (metadata.isGenerateBaseResponse()) {
                registerTarget(metadata.getBaseResponseQualifiedName(), metadata, sourceElement, owners);
            }
            if (metadata.isGenerateListResponse()) {
                registerTarget(metadata.getListResponseQualifiedName(), metadata, sourceElement, owners);
            }
            if (metadata.isGenerateConverter()) {
                registerTarget(metadata.getConverterQualifiedName(), metadata, sourceElement, owners);
            }
        }
    }

    private void validateJavaIdentifier(String label, String value, Element sourceElement) {
        if (value == null
                || !SourceVersion.isIdentifier(value)
                || SourceVersion.isKeyword(value)) {
            throw new ProcessorException(
                    label + " 必须是合法的 Java 标识符且不能是关键字，当前值: " + printable(value),
                    sourceElement
            );
        }
    }

    private void validateResponseAliases(Element sourceElement) {
        for (Element enclosed : sourceElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD
                    || enclosed.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            ResponseAlias alias = enclosed.getAnnotation(ResponseAlias.class);
            if (alias != null) {
                validateJavaIdentifier("@ResponseAlias.value()", alias.value(), enclosed);
            }
        }
    }

    private void validateResponseFieldConflicts(AutoModelMetadata metadata, Element sourceElement) {
        Map<String, Element> fieldsByName = directFieldsByName(sourceElement);
        if (metadata.isGenerateBaseResponse()) {
            validateResponseFieldConflict(
                    metadata.getFields(), FieldMetadata::isGenerateForBaseResponse, fieldsByName
            );
        }
        if (metadata.isGenerateListResponse()) {
            validateResponseFieldConflict(
                    metadata.getFields(), FieldMetadata::isGenerateForListResponse, fieldsByName
            );
        }
    }

    private void validateResponseFieldConflict(Iterable<FieldMetadata> fields,
                                               FieldSelector selector,
                                               Map<String, Element> fieldsByName) {
        Set<String> responseNames = new HashSet<>();
        for (FieldMetadata field : fields) {
            if (!selector.include(field)) {
                continue;
            }
            String responseFieldName = field.getEffectiveResponseFieldName();
            if (!responseNames.add(responseFieldName)) {
                Element fieldElement = fieldsByName.get(field.getSourceFieldName());
                throw new ProcessorException("Response 字段名冲突: " + responseFieldName, fieldElement);
            }
        }
    }

    private Map<String, Element> directFieldsByName(Element sourceElement) {
        Map<String, Element> fields = new HashMap<>();
        for (Element enclosed : sourceElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                fields.put(enclosed.getSimpleName().toString(), enclosed);
            }
        }
        return fields;
    }

    private void registerTarget(String qualifiedName,
                                AutoModelMetadata metadata,
                                Element sourceElement,
                                Map<String, TargetOwner> owners) {
        TargetOwner previous = owners.putIfAbsent(
                qualifiedName,
                new TargetOwner(metadata.getEntityQualifiedName())
        );
        if (previous != null) {
            throw new ProcessorException(
                    "目标类型全限定名冲突: " + qualifiedName
                            + "，实体: " + previous.entityQualifiedName()
                            + " 与 " + metadata.getEntityQualifiedName(),
                    sourceElement
            );
        }
    }

    private String printable(String value) {
        return value == null ? "<null>" : '"' + value + '"';
    }

    @FunctionalInterface
    private interface FieldSelector {
        boolean include(FieldMetadata field);
    }

    private record TargetOwner(String entityQualifiedName) {
    }
}
