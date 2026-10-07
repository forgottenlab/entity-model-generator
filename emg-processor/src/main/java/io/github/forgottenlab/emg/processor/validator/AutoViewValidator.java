package io.github.forgottenlab.emg.processor.validator;

import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.core.model.AutoViewMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.model.SourceMetadata;
import io.github.forgottenlab.emg.processor.support.ProcessorException;
import io.github.forgottenlab.emg.processor.support.ViewOutputProvenance;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EMG V2 自定义 View 的编译期输入与目标校验器。
 */
public class AutoViewValidator {

    private final Elements elements;
    private final ViewOutputProvenance provenance;

    public AutoViewValidator(ProcessingEnvironment processingEnvironment) {
        this.elements = processingEnvironment.getElementUtils();
        this.provenance = new ViewOutputProvenance(processingEnvironment);
    }

    public void validateAnnotatedElement(Element sourceElement) {
        if (sourceElement.getKind() != ElementKind.CLASS) {
            throw new ProcessorException(
                    "@AutoView 只能标注在 class 上: " + sourceElement,
                    sourceElement
            );
        }
    }

    public void validateAnnotations(List<AutoView> annotations, Element sourceElement) {
        for (AutoView annotation : annotations) {
            validateJavaIdentifier("@AutoView.value()", annotation.value(), sourceElement);
            if (!annotation.name().isEmpty()) {
                validateJavaIdentifier("@AutoView.name()", annotation.name(), sourceElement);
            }
        }
    }

    public void validateFieldGroups(SourceMetadata sourceMetadata, Element sourceElement) {
        Map<String, Element> fieldsByName = directFieldsByName(sourceElement);
        for (FieldMetadata field : sourceMetadata.getFields()) {
            Element fieldElement = fieldsByName.getOrDefault(field.getSourceFieldName(), sourceElement);
            for (String group : field.getViewGroups()) {
                validateJavaIdentifier("@ViewGroups.value()", group, fieldElement);
            }
        }
    }

    public void validateViews(List<AutoViewMetadata> views, Element sourceElement) {
        for (AutoViewMetadata view : views) {
            validateJavaIdentifier("View 目标类名", view.getViewClassName(), sourceElement);
            if (view.getFields().isEmpty()) {
                throw new ProcessorException(
                        "@AutoView 引用的字段分组不存在或不包含任何字段: " + view.getGroup(),
                        sourceElement
                );
            }
        }
    }

    /**
     * 校验全部目标，返回已由本地 EMG 输出且模型完全匹配、可安全复用的 View。
     */
    public Set<AutoViewMetadata> validateTargetTypeConflicts(Map<AutoViewMetadata, Element> candidates) {
        Map<String, String> owners = new LinkedHashMap<>();
        Set<AutoViewMetadata> reusable = new LinkedHashSet<>();
        for (Map.Entry<AutoViewMetadata, Element> candidate : candidates.entrySet()) {
            AutoViewMetadata metadata = candidate.getKey();
            Element sourceElement = candidate.getValue();
            String qualifiedName = metadata.getViewQualifiedName();
            String previousOwner = owners.putIfAbsent(qualifiedName, metadata.getSourceQualifiedName());
            if (previousOwner != null) {
                throw new ProcessorException(
                        "View 目标类型全限定名冲突: " + qualifiedName
                                + "，来源: " + previousOwner
                                + " 与 " + metadata.getSourceQualifiedName(),
                        sourceElement
                );
            }
            TypeElement existing = elements.getTypeElement(qualifiedName);
            ViewOutputProvenance.ExistingTarget classification = provenance.classify(existing, metadata);
            if (classification == ViewOutputProvenance.ExistingTarget.EXTERNAL) {
                throw new ProcessorException(
                        "View 目标类型已存在: " + qualifiedName
                                + " (user/dependency type or unverified legacy output; "
                                + "clean regeneration is required for outputs predating EMG provenance)",
                        sourceElement
                );
            }
            if (classification == ViewOutputProvenance.ExistingTarget.HISTORICAL_EMG) {
                if (!provenance.matchesDesiredModel(existing, metadata)) {
                    throw new ProcessorException(
                            "EMG generated type is stale; clean regeneration is required: " + qualifiedName,
                            sourceElement
                    );
                }
                reusable.add(metadata);
            }
        }
        return reusable;
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

    private Map<String, Element> directFieldsByName(Element sourceElement) {
        Map<String, Element> fields = new LinkedHashMap<>();
        for (Element enclosed : sourceElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                fields.put(enclosed.getSimpleName().toString(), enclosed);
            }
        }
        return fields;
    }

    private String printable(String value) {
        return value == null ? "<null>" : '"' + value + '"';
    }
}
