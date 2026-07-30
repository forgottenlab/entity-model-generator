package io.github.forgottenlab.emg.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 触发指定字段分组的单表 View 生成。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(AutoViews.class)
public @interface AutoView {

    /**
     * 要生成的字段分组。
     */
    String value();

    /**
     * 最终生成类的完整简单类名；为空时使用默认命名约定。
     */
    String name() default "";
}
