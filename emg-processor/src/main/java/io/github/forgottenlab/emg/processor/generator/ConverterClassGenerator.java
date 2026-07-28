package io.github.forgottenlab.emg.processor.generator;


import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.util.NameUtils;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Converter 生成器。
 *
 * <p>负责生成实体到 DTO / Response 的静态转换器类。</p>
 *
 * <p>当前会按配置生成以下方法：</p>
 * <ul>
 *     <li>{@code toDto}</li>
 *     <li>{@code toBaseResponse}</li>
 *     <li>{@code toListResponse}</li>
 *     <li>{@code toDtoList}</li>
 *     <li>{@code toBaseResponseList}</li>
 *     <li>{@code toListResponseList}</li>
 * </ul>
 */
public class ConverterClassGenerator extends AbstractClassGenerator {

    public ConverterClassGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    /**
     * 生成 Converter 源码。
     *
     * @param metadata 当前实体的生成元数据
     */
    public void generate(AutoModelMetadata metadata) {
        String className = metadata.getConverterClassName();
        String entityClass = metadata.getEntityClassName();
        String dtoClass = metadata.getDtoClassName();
        String baseResponseClass = metadata.getBaseResponseClassName();
        String listResponseClass = metadata.getListResponseClassName();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(metadata.getConverterPackageName()).append(";\n\n")
                .append("import javax.annotation.processing.Generated;\n")
                .append("import java.util.ArrayList;\n")
                .append("import java.util.List;\n")
                .append("import ").append(metadata.getEntityQualifiedName()).append(";\n");

        if (metadata.isGenerateDto()) {
            sb.append("import ").append(metadata.getDtoQualifiedName()).append(";\n");
        }
        if (metadata.isGenerateBaseResponse()) {
            sb.append("import ").append(metadata.getBaseResponseQualifiedName()).append(";\n");
        }
        if (metadata.isGenerateListResponse()) {
            sb.append("import ").append(metadata.getListResponseQualifiedName()).append(";\n");
        }

        sb.append("\n")
                .append("/**\n")
                .append(" * ").append(className).append("\n")
                .append(" * 由 AutoGen V1 编译期自动生成，请勿手动修改。\n")
                .append(" */\n")
                .append("@Generated(\"AutoGen V1\")\n")
                .append("public final class ").append(className).append(" {\n\n")
                .append("    private ").append(className).append("() {\n")
                .append("    }\n\n");

        if (metadata.isGenerateDto()) {
            appendConvertMethod(sb, entityClass, dtoClass, "toDto", metadata.getFields(),
                    FieldMetadata::isGenerateForDto, false);
            appendConvertListMethod(sb, entityClass, dtoClass, "toDtoList", "toDto");
        }

        if (metadata.isGenerateBaseResponse()) {
            appendConvertMethod(sb, entityClass, baseResponseClass, "toBaseResponse", metadata.getFields(),
                    FieldMetadata::isGenerateForBaseResponse, true);
            appendConvertListMethod(sb, entityClass, baseResponseClass, "toBaseResponseList", "toBaseResponse");
        }

        if (metadata.isGenerateListResponse()) {
            appendConvertMethod(sb, entityClass, listResponseClass, "toListResponse", metadata.getFields(),
                    FieldMetadata::isGenerateForListResponse, true);
            appendConvertListMethod(sb, entityClass, listResponseClass, "toListResponseList", "toListResponse");
        }

        sb.append("}\n");
        writeJavaFile(metadata.getConverterPackageName(), className, sb.toString());
    }

    /**
     * 生成单对象转换方法。
     */
    private void appendConvertMethod(StringBuilder sb,
                                     String sourceClass,
                                     String targetClass,
                                     String methodName,
                                     List<FieldMetadata> fields,
                                     Predicate<FieldMetadata> includePredicate,
                                     boolean useResponseFieldName) {
        List<FieldMetadata> selected = new ArrayList<>();
        for (FieldMetadata field : fields) {
            if (includePredicate.test(field)) {
                selected.add(field);
            }
        }

        sb.append("    public static ").append(targetClass).append(" ").append(methodName)
                .append("(").append(sourceClass).append(" source) {\n")
                .append("        if (source == null) {\n")
                .append("            return null;\n")
                .append("        }\n")
                .append("        ").append(targetClass).append(" target = new ").append(targetClass).append("();\n");

        for (FieldMetadata field : selected) {
            String sourceField = field.getSourceFieldName();
            String targetField = useResponseFieldName
                    ? field.getEffectiveResponseFieldName()
                    : field.getSourceFieldName();

            sb.append("        target.set").append(NameUtils.capitalize(targetField))
                    .append("(source.get").append(NameUtils.capitalize(sourceField)).append("());\n");
        }

        sb.append("        return target;\n")
                .append("    }\n\n");
    }

    /**
     * 生成列表转换方法。
     */
    private void appendConvertListMethod(StringBuilder sb,
                                         String sourceClass,
                                         String targetClass,
                                         String methodName,
                                         String singleConvertMethodName) {
        sb.append("    public static List<").append(targetClass).append("> ").append(methodName)
                .append("(List<").append(sourceClass).append("> sourceList) {\n")
                .append("        if (sourceList == null) {\n")
                .append("            return null;\n")
                .append("        }\n")
                .append("        List<").append(targetClass).append("> result = new ArrayList<>();\n")
                .append("        for (").append(sourceClass).append(" item : sourceList) {\n")
                .append("            result.add(").append(singleConvertMethodName).append("(item));\n")
                .append("        }\n")
                .append("        return result;\n")
                .append("    }\n\n");
    }
}