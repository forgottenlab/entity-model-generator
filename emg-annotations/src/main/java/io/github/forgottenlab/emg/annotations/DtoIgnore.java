package io.github.forgottenlab.emg.annotations;

import java.lang.annotation.*;

/**
 * 标记字段不生成到 DTO 中。
 *
 * <p>适用于该字段仅用于持久化或内部处理，
 * 但不希望出现在自动生成的 DTO 中的场景。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DtoIgnore {
}