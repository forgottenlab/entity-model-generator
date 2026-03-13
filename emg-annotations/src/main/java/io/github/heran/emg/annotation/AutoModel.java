package io.github.heran.emg.annotation;

import java.lang.annotation.*;

/**
 * 标记当前实体类参与 AutoGen 的编译期模型生成。
 *
 * <p>该注解用于声明：以当前实体类为字段来源，自动生成一组标准模型，
 * 例如 DTO、BaseResponse、ListResponse 以及 Converter。</p>
 *
 * <p>适用于单表实体驱动的标准模型生成场景。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoModel {

    /**
     * 生成类名前缀。
     *
     * <p>例如：{@code User -> UserDTO / UserBaseResponse / UserListResponse}</p>
     */
    String value();

    /**
     * 是否生成 DTO。
     */
    boolean generateDto() default true;

    /**
     * 是否生成基础响应对象。
     *
     * <p>通常用于详情页或通用展示场景。</p>
     */
    boolean generateBaseResponse() default true;

    /**
     * 是否生成列表响应对象。
     *
     * <p>通常用于列表页展示，会结合 {@link ListIgnore} 进行字段裁剪。</p>
     */
    boolean generateListResponse() default true;

    /**
     * 是否生成转换器。
     *
     * <p>例如生成 {@code UserConverter}，用于 Entity 到 DTO / Response 的转换。</p>
     */
    boolean generateConverter() default true;
}