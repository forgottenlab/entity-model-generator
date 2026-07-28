package io.github.forgottenlab.emg.annotations;

import java.lang.annotation.*;

/**
 * 指定字段在自动生成的 Response 中使用的别名。
 *
 * <p>适用于实体字段名与对外返回字段名不一致的场景。</p>
 *
 * <p>例如：{@code nickname -> name}</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ResponseAlias {

    /**
     * Response 中使用的目标字段名。
     */
    String value();
}