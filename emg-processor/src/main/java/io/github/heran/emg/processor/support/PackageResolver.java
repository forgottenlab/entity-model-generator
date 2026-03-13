package io.github.heran.emg.processor.support;

import io.github.heran.emg.core.constant.ModelConstants;

/**
 * 生成包路径解析器。
 *
 * <p>用于根据实体包路径推导 DTO、Response、Converter 的目标包路径。</p>
 *
 * <p>例如：</p>
 * <pre>
 * com.example.user.entity
 *   -> com.example.user.model.dto
 *   -> com.example.user.model.response
 *   -> com.example.user.converter
 * </pre>
 */
public final class PackageResolver {

    private PackageResolver() {
    }

    /**
     * 解析 DTO 包路径。
     */
    public static String resolveDtoPackage(String entityPackage) {
        return replaceEntityPackage(entityPackage, ModelConstants.DTO_PACKAGE_SEGMENT);
    }

    /**
     * 解析 Response 包路径。
     */
    public static String resolveResponsePackage(String entityPackage) {
        return replaceEntityPackage(entityPackage, ModelConstants.RESPONSE_PACKAGE_SEGMENT);
    }

    /**
     * 解析 Converter 包路径。
     */
    public static String resolveConverterPackage(String entityPackage) {
        return replaceEntityPackage(entityPackage, ModelConstants.CONVERTER_PACKAGE_SEGMENT);
    }

    /**
     * 将实体包路径替换为目标包段。
     *
     * <p>若当前包以 {@code .entity} 结尾，则替换该后缀；
     * 否则直接在原包名后追加目标包段。</p>
     */
    private static String replaceEntityPackage(String entityPackage, String targetSegment) {
        if (entityPackage.endsWith(ModelConstants.ENTITY_PACKAGE_SEGMENT)) {
            return entityPackage.substring(
                    0,
                    entityPackage.length() - ModelConstants.ENTITY_PACKAGE_SEGMENT.length()
            ) + targetSegment;
        }
        return entityPackage + targetSegment;
    }
}