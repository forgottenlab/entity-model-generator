package io.github.forgottenlab.emg.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个自定义 View 的生成元数据。
 */
public class AutoViewMetadata {

    private String sourceQualifiedName;

    private String group;

    private String viewPackageName;

    private String viewClassName;

    private final List<FieldMetadata> fields = new ArrayList<>();

    public String getSourceQualifiedName() {
        return sourceQualifiedName;
    }

    public void setSourceQualifiedName(String sourceQualifiedName) {
        this.sourceQualifiedName = sourceQualifiedName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getViewPackageName() {
        return viewPackageName;
    }

    public void setViewPackageName(String viewPackageName) {
        this.viewPackageName = viewPackageName;
    }

    public String getViewClassName() {
        return viewClassName;
    }

    public void setViewClassName(String viewClassName) {
        this.viewClassName = viewClassName;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public String getViewQualifiedName() {
        return viewPackageName + "." + viewClassName;
    }
}
