package io.github.forgottenlab.emg.core.model;

import io.github.forgottenlab.emg.core.util.NameUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动生成模型的整体元数据。
 *
 * <p>描述一个实体类在 AutoGen 中的完整生成配置。</p>
 */
public class AutoModelMetadata {

    /**
     * 实体类所在包名。
     */
    private String entityPackageName;

    /**
     * 实体类名。
     */
    private String entityClassName;

    /**
     * 模型前缀。
     *
     * <p>例如：User</p>
     */
    private String modelPrefix;

    /**
     * DTO 包名。
     */
    private String dtoPackageName;

    /**
     * Response 包名。
     */
    private String responsePackageName;

    /**
     * Converter 包名。
     */
    private String converterPackageName;

    /**
     * 是否生成 DTO。
     */
    private boolean generateDto;

    /**
     * 是否生成 BaseResponse。
     */
    private boolean generateBaseResponse;

    /**
     * 是否生成 ListResponse。
     */
    private boolean generateListResponse;

    /**
     * 是否生成 Converter。
     */
    private boolean generateConverter;

    /**
     * 当前实体的字段元数据集合。
     */
    private final List<FieldMetadata> fields = new ArrayList<>();

    public String getEntityPackageName() {
        return entityPackageName;
    }

    public void setEntityPackageName(String entityPackageName) {
        this.entityPackageName = entityPackageName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    public String getModelPrefix() {
        return modelPrefix;
    }

    public void setModelPrefix(String modelPrefix) {
        this.modelPrefix = modelPrefix;
    }

    public String getDtoPackageName() {
        return dtoPackageName;
    }

    public void setDtoPackageName(String dtoPackageName) {
        this.dtoPackageName = dtoPackageName;
    }

    public String getResponsePackageName() {
        return responsePackageName;
    }

    public void setResponsePackageName(String responsePackageName) {
        this.responsePackageName = responsePackageName;
    }

    public String getConverterPackageName() {
        return converterPackageName;
    }

    public void setConverterPackageName(String converterPackageName) {
        this.converterPackageName = converterPackageName;
    }

    public boolean isGenerateDto() {
        return generateDto;
    }

    public void setGenerateDto(boolean generateDto) {
        this.generateDto = generateDto;
    }

    public boolean isGenerateBaseResponse() {
        return generateBaseResponse;
    }

    public void setGenerateBaseResponse(boolean generateBaseResponse) {
        this.generateBaseResponse = generateBaseResponse;
    }

    public boolean isGenerateListResponse() {
        return generateListResponse;
    }

    public void setGenerateListResponse(boolean generateListResponse) {
        this.generateListResponse = generateListResponse;
    }

    public boolean isGenerateConverter() {
        return generateConverter;
    }

    public void setGenerateConverter(boolean generateConverter) {
        this.generateConverter = generateConverter;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    /**
     * 获取实体全限定名。
     */
    public String getEntityQualifiedName() {
        return entityPackageName + "." + entityClassName;
    }

    /**
     * 获取 DTO 类名。
     */
    public String getDtoClassName() {
        return NameUtils.dtoName(modelPrefix);
    }

    /**
     * 获取 BaseResponse 类名。
     */
    public String getBaseResponseClassName() {
        return NameUtils.baseResponseName(modelPrefix);
    }

    /**
     * 获取 ListResponse 类名。
     */
    public String getListResponseClassName() {
        return NameUtils.listResponseName(modelPrefix);
    }

    /**
     * 获取 Converter 类名。
     */
    public String getConverterClassName() {
        return NameUtils.converterName(modelPrefix);
    }

    /**
     * 获取 DTO 全限定名。
     */
    public String getDtoQualifiedName() {
        return dtoPackageName + "." + getDtoClassName();
    }

    /**
     * 获取 BaseResponse 全限定名。
     */
    public String getBaseResponseQualifiedName() {
        return responsePackageName + "." + getBaseResponseClassName();
    }

    /**
     * 获取 ListResponse 全限定名。
     */
    public String getListResponseQualifiedName() {
        return responsePackageName + "." + getListResponseClassName();
    }

    /**
     * 获取 Converter 全限定名。
     */
    public String getConverterQualifiedName() {
        return converterPackageName + "." + getConverterClassName();
    }
}