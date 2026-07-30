package io.github.forgottenlab.emg.processor.generator;

import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.util.NameUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 所有源码生成器的抽象父类。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>提供统一的源码写入能力</li>
 *     <li>提供通用的 Java 类源码拼装模板</li>
 *     <li>处理 import 输出、getter/setter 输出等重复逻辑</li>
 * </ul>
 *
 * <p>当前版本仍采用字符串拼接生成源码，这样实现简单、便于理解，
 * 适合 AutoGen V1 的目标。</p>
 */
public abstract class AbstractClassGenerator {

    /**
     * APT 处理环境。
     *
     * <p>用于获取 {@link Filer}、日志工具等编译期能力。</p>
     */
    protected final ProcessingEnvironment processingEnvironment;

    protected AbstractClassGenerator(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * 将生成好的源码写入编译器输出目录。
     *
     * @param packageName 目标包名
     * @param className   目标类名
     * @param content     完整源码内容
     */
    protected void writeJavaFile(String packageName, String className, String content) {
        Filer filer = processingEnvironment.getFiler();
        try {
            JavaFileObject fileObject = filer.createSourceFile(packageName + "." + className);
            System.out.println("[AutoGen] writing to: " + fileObject.toUri());
            try (Writer writer = fileObject.openWriter()) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("生成源码失败: " + packageName + "." + className, e);
        }
    }

    /**
     * 根据字段元数据生成一个完整的 Java Bean 类源码。
     *
     * @param packageName          目标包名
     * @param className            类名
     * @param classComment         类注释标题
     * @param fields               字段元数据列表
     * @param includePredicate     字段筛选条件
     * @param useResponseFieldName 是否使用 Response 字段名
     * @return 完整 Java 源码字符串
     */
    protected String buildClassSource(String packageName,
                                      String className,
                                      String classComment,
                                      List<FieldMetadata> fields,
                                      Predicate<FieldMetadata> includePredicate,
                                      boolean useResponseFieldName) {
        return buildClassSource(
                packageName,
                className,
                classComment,
                fields,
                includePredicate,
                useResponseFieldName,
                "AutoGen V1"
        );
    }

    protected String buildClassSource(String packageName,
                                      String className,
                                      String classComment,
                                      List<FieldMetadata> fields,
                                      Predicate<FieldMetadata> includePredicate,
                                      boolean useResponseFieldName,
                                      String generatorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        Set<String> ambiguousSimpleNames = resolveAmbiguousSimpleNames(fields, includePredicate);

        appendGeneratedImport(sb);
        appendImports(sb, fields, includePredicate, ambiguousSimpleNames);

        sb.append("/**\n")
                .append(" * ").append(classComment).append("\n")
                .append(" * 由 ").append(generatorName).append(" 编译期自动生成，请勿手动修改。\n")
                .append(" */\n")
                .append("@Generated(\"").append(generatorName).append("\")\n")
                .append("public class ").append(className).append(" {\n\n");

        // 生成字段定义
        for (FieldMetadata field : fields) {
            if (!includePredicate.test(field)) {
                continue;
            }

            String fieldName = useResponseFieldName
                    ? field.getEffectiveResponseFieldName()
                    : field.getSourceFieldName();

            sb.append("    /**\n")
                    .append("     * 自动生成字段。\n")
                    .append("     */\n")
                    .append("    private ").append(displayTypeName(field, ambiguousSimpleNames))
                    .append(" ").append(fieldName).append(";\n\n");
        }

        // 生成 getter / setter
        for (FieldMetadata field : fields) {
            if (!includePredicate.test(field)) {
                continue;
            }

            String fieldName = useResponseFieldName
                    ? field.getEffectiveResponseFieldName()
                    : field.getSourceFieldName();

            String methodSuffix = NameUtils.capitalize(fieldName);
            String typeName = displayTypeName(field, ambiguousSimpleNames);

            sb.append("    public ").append(typeName).append(" get").append(methodSuffix).append("() {\n")
                    .append("        return ").append(fieldName).append(";\n")
                    .append("    }\n\n");

            sb.append("    public void set").append(methodSuffix)
                    .append("(").append(typeName).append(" ").append(fieldName).append(") {\n")
                    .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                    .append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 输出 {@code @Generated} 注解所需的 import。
     */
    private void appendGeneratedImport(StringBuilder sb) {
        sb.append("import javax.annotation.processing.Generated;\n\n");
    }

    /**
     * 根据字段元数据输出 import 语句，并自动去重。
     *
     * <p>当前会过滤：</p>
     * <ul>
     *     <li>{@code java.lang.*}</li>
     *     <li>未命中的字段</li>
     *     <li>重复类型导入</li>
     * </ul>
     */
    private void appendImports(StringBuilder sb,
                               List<FieldMetadata> fields,
                               Predicate<FieldMetadata> includePredicate,
                               Set<String> ambiguousSimpleNames) {
        Set<String> imports = new LinkedHashSet<>();

        for (FieldMetadata field : fields) {
            if (!includePredicate.test(field)) {
                continue;
            }

            if (usesQualifiedTypeName(field, ambiguousSimpleNames)) {
                continue;
            }
            for (String referencedTypeName : field.getReferencedTypeNames()) {
                if (!referencedTypeName.startsWith("java.lang.")) {
                    imports.add(referencedTypeName);
                }
            }
        }

        for (String importType : imports) {
            sb.append("import ").append(importType).append(";\n");
        }

        if (!imports.isEmpty()) {
            sb.append("\n");
        }
    }

    private Set<String> resolveAmbiguousSimpleNames(List<FieldMetadata> fields,
                                                    Predicate<FieldMetadata> includePredicate) {
        Map<String, String> qualifiedNameBySimpleName = new HashMap<>();
        Set<String> ambiguousSimpleNames = new LinkedHashSet<>();

        for (FieldMetadata field : fields) {
            if (!includePredicate.test(field)) {
                continue;
            }
            for (String referencedTypeName : field.getReferencedTypeNames()) {
                String simpleName = simpleName(referencedTypeName);
                String previous = qualifiedNameBySimpleName.putIfAbsent(simpleName, referencedTypeName);
                if (previous != null && !previous.equals(referencedTypeName)) {
                    ambiguousSimpleNames.add(simpleName);
                }
            }
        }
        return ambiguousSimpleNames;
    }

    private String displayTypeName(FieldMetadata field, Set<String> ambiguousSimpleNames) {
        return usesQualifiedTypeName(field, ambiguousSimpleNames)
                ? field.getQualifiedTypeName()
                : field.getDisplayTypeName();
    }

    private boolean usesQualifiedTypeName(FieldMetadata field, Set<String> ambiguousSimpleNames) {
        return field.getReferencedTypeNames().stream()
                .map(this::simpleName)
                .anyMatch(ambiguousSimpleNames::contains);
    }

    private String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
    }
}
