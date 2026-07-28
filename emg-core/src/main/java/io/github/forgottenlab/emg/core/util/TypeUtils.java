package io.github.forgottenlab.emg.core.util;

/**
 * 类型工具类。
 */
public final class TypeUtils {

    private TypeUtils() {
    }

    /**
     * 获取全限定类型名的简单类名。
     *
     * <p>例如：{@code java.time.LocalDateTime -> LocalDateTime}</p>
     */
    public static String simpleName(String qualifiedTypeName) {
        if (qualifiedTypeName == null || qualifiedTypeName.isBlank()) {
            return qualifiedTypeName;
        }
        int index = qualifiedTypeName.lastIndexOf('.');
        return index < 0 ? qualifiedTypeName : qualifiedTypeName.substring(index + 1);
    }

    /**
     * 获取全限定类型名的包名。
     *
     * <p>例如：{@code java.time.LocalDateTime -> java.time}</p>
     */
    public static String packageName(String qualifiedTypeName) {
        if (qualifiedTypeName == null || qualifiedTypeName.isBlank()) {
            return qualifiedTypeName;
        }
        int index = qualifiedTypeName.lastIndexOf('.');
        return index < 0 ? "" : qualifiedTypeName.substring(0, index);
    }

    /**
     * 判断是否为全限定类名。
     */
    public static boolean isQualifiedName(String typeName) {
        return typeName != null && typeName.contains(".");
    }
}