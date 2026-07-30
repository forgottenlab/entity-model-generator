package io.github.forgottenlab.emg.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个模型来源类型的一次性解析结果。
 */
public class SourceMetadata {

    private String packageName;

    private String className;

    private final List<FieldMetadata> fields = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public String getQualifiedName() {
        return packageName + "." + className;
    }
}
