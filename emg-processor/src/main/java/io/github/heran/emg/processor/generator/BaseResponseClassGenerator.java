package io.github.heran.emg.processor.generator;


import io.github.heran.emg.core.model.AutoModelMetadata;
import io.github.heran.emg.core.model.FieldMetadata;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * BaseResponse 生成器。
 *
 * <p>负责根据实体元数据生成：</p>
 * <ul>
 *     <li>{@code XxxBaseResponse}</li>
 * </ul>
 *
 * <p>BaseResponse 通常用于：</p>
 * <ul>
 *     <li>详情页展示</li>
 *     <li>通用单对象返回</li>
 *     <li>作为业务扩展 Response 的父类</li>
 * </ul>
 */
public class BaseResponseClassGenerator extends AbstractClassGenerator {

    public BaseResponseClassGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    /**
     * 生成基础响应对象源码。
     *
     * @param metadata 当前实体的生成元数据
     */
    public void generate(AutoModelMetadata metadata) {
        String className = metadata.getBaseResponseClassName();
        String source = buildClassSource(
                metadata.getResponsePackageName(),
                className,
                className + "，适合详情或通用展示。",
                metadata.getFields(),
                FieldMetadata::isGenerateForBaseResponse,
                true
        );
        writeJavaFile(metadata.getResponsePackageName(), className, source);
    }
}