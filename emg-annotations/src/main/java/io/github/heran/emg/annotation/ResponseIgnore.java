package io.github.heran.emg.annotation;

import java.lang.annotation.*;

/**
 * 标记字段不生成到所有 Response 中。
 *
 * <p>适用于密码、内部标记字段、敏感字段等不应对外暴露的场景。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ResponseIgnore {
}