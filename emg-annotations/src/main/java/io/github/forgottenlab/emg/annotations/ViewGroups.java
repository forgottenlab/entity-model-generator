package io.github.forgottenlab.emg.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段所属的自定义 View 分组。
 *
 * <p>字段可以同时属于多个分组；只有实体类显式声明对应的
 * {@link AutoView} 时才会生成 View。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface ViewGroups {

    /**
     * 当前字段所属的一个或多个分组。
     */
    String[] value();
}
