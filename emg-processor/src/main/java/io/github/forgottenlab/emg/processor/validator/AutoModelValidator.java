package io.github.forgottenlab.emg.processor.validator;

import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AutoGen 编译期校验器。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>校验 {@code @AutoModel.value()} 是否为空</li>
 *     <li>校验模型前缀是否符合 Java 类名前缀规范</li>
 *     <li>校验注解是否只标注在类上</li>
 *     <li>校验 Response 字段别名是否发生冲突</li>
 * </ul>
 *
 * <p>该类的目标是尽可能在编译阶段尽早暴露错误，
 * 避免生成出非法源码或在运行期才发现问题。</p>
 */
public class AutoModelValidator {

    /**
     * Java 类名前缀的基础校验规则。
     *
     * <p>例如允许：User、UserInfo、User123</p>
     * <p>例如不允许：user-name、123User</p>
     */
    private static final Pattern JAVA_CLASS_NAME_PATTERN = Pattern.compile("[A-Z][A-Za-z0-9_]*");

    /**
     * 处理环境。
     *
     * <p>当前版本暂未直接使用，但保留该引用以便未来扩展更多校验能力。</p>
     */
    @SuppressWarnings("unused")
    private final ProcessingEnvironment processingEnvironment;

    public AutoModelValidator(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * 对单个实体的元数据执行完整校验。
     *
     * @param metadata     已解析出的实体元数据
     * @param sourceElement 源实体元素
     */
    public void validate(AutoModelMetadata metadata, Element sourceElement) {
        validateAnnotatedElement(sourceElement);
        validateModelPrefix(metadata, sourceElement);
        validateResponseFieldConflict(metadata);
    }

    /**
     * 校验 {@code @AutoModel} 只能标注在类上。
     */
    private void validateAnnotatedElement(Element sourceElement) {
        if (sourceElement.getKind() != ElementKind.CLASS) {
            throw new IllegalArgumentException("@AutoModel 只能标注在类上: " + sourceElement);
        }
    }

    /**
     * 校验模型前缀是否合法。
     *
     * <p>要求：</p>
     * <ul>
     *     <li>不能为空</li>
     *     <li>符合 Java 类名前缀规范</li>
     * </ul>
     */
    private void validateModelPrefix(AutoModelMetadata metadata, Element sourceElement) {
        String modelPrefix = metadata.getModelPrefix();

        if (modelPrefix == null || modelPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("@AutoModel.value() 不能为空: " + sourceElement);
        }

        if (!JAVA_CLASS_NAME_PATTERN.matcher(modelPrefix).matches()) {
            throw new IllegalArgumentException(
                    "@AutoModel.value() 必须符合 Java 类名前缀规范，例如 User / UserInfo，当前值: " + modelPrefix
            );
        }
    }

    /**
     * 校验 Response 字段名冲突。
     *
     * <p>例如下面两种情况会冲突：</p>
     * <ul>
     *     <li>实体中本来就有字段 {@code name}</li>
     *     <li>另一个字段通过 {@code @ResponseAlias("name")} 也映射为 {@code name}</li>
     * </ul>
     *
     * <p>当前只对 BaseResponse 的字段冲突做严格校验，
     * 因为它是所有 Response 生成的基础来源之一。</p>
     */
    private void validateResponseFieldConflict(AutoModelMetadata metadata) {
        Set<String> responseNames = new HashSet<>();

        for (FieldMetadata field : metadata.getFields()) {
            if (!field.isGenerateForBaseResponse()) {
                continue;
            }

            String responseFieldName = field.getEffectiveResponseFieldName();
            if (!responseNames.add(responseFieldName)) {
                throw new IllegalArgumentException("Response 字段名冲突: " + responseFieldName);
            }
        }
    }
}