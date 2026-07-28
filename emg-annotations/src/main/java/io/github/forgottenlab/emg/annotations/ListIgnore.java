package io.github.forgottenlab.emg.annotations;

import java.lang.annotation.*;

/**
 * 标记字段不生成到 ListResponse 中。
 *
 * <p>适用于详情页需要展示、但列表页不需要展示的字段，
 * 例如创建时间、更新时间、备注、大文本内容等。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ListIgnore {
}