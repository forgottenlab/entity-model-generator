package io.github.forgottenlab.emg.core.util;

import io.github.forgottenlab.emg.core.constant.ModelConstants;

/**
 * 命名工具类。
 */
public final class NameUtils {

    private NameUtils() {
    }

    /**
     * 生成 DTO 类名。
     */
    public static String dtoName(String prefix) {
        return prefix + ModelConstants.DTO_SUFFIX;
    }

    /**
     * 生成 BaseResponse 类名。
     */
    public static String baseResponseName(String prefix) {
        return prefix + ModelConstants.BASE_RESPONSE_SUFFIX;
    }

    /**
     * 生成 ListResponse 类名。
     */
    public static String listResponseName(String prefix) {
        return prefix + ModelConstants.LIST_RESPONSE_SUFFIX;
    }

    /**
     * 生成 Converter 类名。
     */
    public static String converterName(String prefix) {
        return prefix + ModelConstants.CONVERTER_SUFFIX;
    }

    /**
     * 将首字母转换为大写。
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * 将首字母转换为小写。
     */
    public static String uncapitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * 去掉默认实体后缀。
     *
     * <p>例如：{@code UserEntity -> User}</p>
     */
    public static String removeEntitySuffix(String className) {
        if (className == null || className.isBlank()) {
            return className;
        }
        if (className.endsWith(ModelConstants.ENTITY_SUFFIX)) {
            return className.substring(0, className.length() - ModelConstants.ENTITY_SUFFIX.length());
        }
        return className;
    }
}