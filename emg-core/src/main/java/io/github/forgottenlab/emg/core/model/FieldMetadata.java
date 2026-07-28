package io.github.forgottenlab.emg.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体字段元数据。
 *
 * <p>用于描述单个字段在 DTO、BaseResponse、ListResponse 中的生成行为。</p>
 */
public class FieldMetadata {

    /**
     * 实体中的原始字段名。
     */
    private String sourceFieldName;

    /**
     * Response 中使用的字段名。
     *
     * <p>如果未设置别名，通常与 {@link #sourceFieldName} 一致。</p>
     */
    private String responseFieldName;

    /**
     * 使用全限定声明类型组成的字段类型源码表达式。
     *
     * <p>例如：{@code java.time.LocalDateTime[]}、
     * {@code java.util.List<java.lang.String>}</p>
     */
    private String qualifiedTypeName;

    /**
     * 使用简单声明类型组成的字段类型源码表达式。
     *
     * <p>例如：{@code LocalDateTime[]}、{@code List<String>}</p>
     */
    private String simpleTypeName;

    /**
     * 字段类型表达式引用的顶层声明类型。
     *
     * <p>例如 {@code Map<String, Outer.Inner>} 会记录
     * {@code java.util.Map}、{@code java.lang.String} 和 {@code example.Outer}。
     * 生成器据此输出合法 import，并检测 simple name 冲突。</p>
     */
    private final List<String> referencedTypeNames = new ArrayList<>();

    /**
     * Converter 读取源实体字段时实际调用的 getter 名称。
     */
    private String sourceGetterName;

    /**
     * 是否生成到 DTO。
     */
    private boolean generateForDto;

    /**
     * 是否生成到 BaseResponse。
     */
    private boolean generateForBaseResponse;

    /**
     * 是否生成到 ListResponse。
     */
    private boolean generateForListResponse;

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getResponseFieldName() {
        return responseFieldName;
    }

    public void setResponseFieldName(String responseFieldName) {
        this.responseFieldName = responseFieldName;
    }

    public String getQualifiedTypeName() {
        return qualifiedTypeName;
    }

    public void setQualifiedTypeName(String qualifiedTypeName) {
        this.qualifiedTypeName = qualifiedTypeName;
    }

    public String getSimpleTypeName() {
        return simpleTypeName;
    }

    public void setSimpleTypeName(String simpleTypeName) {
        this.simpleTypeName = simpleTypeName;
    }

    public List<String> getReferencedTypeNames() {
        return referencedTypeNames;
    }

    public String getSourceGetterName() {
        return sourceGetterName;
    }

    public void setSourceGetterName(String sourceGetterName) {
        this.sourceGetterName = sourceGetterName;
    }

    public boolean isGenerateForDto() {
        return generateForDto;
    }

    public void setGenerateForDto(boolean generateForDto) {
        this.generateForDto = generateForDto;
    }

    public boolean isGenerateForBaseResponse() {
        return generateForBaseResponse;
    }

    public void setGenerateForBaseResponse(boolean generateForBaseResponse) {
        this.generateForBaseResponse = generateForBaseResponse;
    }

    public boolean isGenerateForListResponse() {
        return generateForListResponse;
    }

    public void setGenerateForListResponse(boolean generateForListResponse) {
        this.generateForListResponse = generateForListResponse;
    }

    /**
     * 是否配置了 Response 字段别名。
     */
    public boolean hasResponseAlias() {
        return responseFieldName != null
                && sourceFieldName != null
                && !responseFieldName.equals(sourceFieldName);
    }

    /**
     * 获取 Response 中最终生效的字段名。
     */
    public String getEffectiveResponseFieldName() {
        return responseFieldName == null || responseFieldName.isBlank()
                ? sourceFieldName
                : responseFieldName;
    }

    /**
     * 获取字段类型显示名。
     *
     * <p>优先返回简单类名，若为空则回退到全限定名。</p>
     */
    public String getDisplayTypeName() {
        return (simpleTypeName != null && !simpleTypeName.isBlank())
                ? simpleTypeName
                : qualifiedTypeName;
    }
}
