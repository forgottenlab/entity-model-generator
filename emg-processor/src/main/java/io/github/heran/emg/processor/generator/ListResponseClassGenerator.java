package io.github.heran.emg.processor.generator;


import io.github.heran.emg.core.model.AutoModelMetadata;
import io.github.heran.emg.core.model.FieldMetadata;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * ListResponse 生成器。
 *
 * <p>负责根据实体元数据生成：</p>
 * <ul>
 *     <li>{@code XxxListResponse}</li>
 * </ul>
 *
 * <p>ListResponse 适用于列表页、分页结果、轻量级数据展示场景。</p>
 */
public class ListResponseClassGenerator extends AbstractClassGenerator {

    public ListResponseClassGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    /**
     * 生成列表响应对象源码。
     *
     * @param metadata 当前实体的生成元数据
     */
    public void generate(AutoModelMetadata metadata) {
        String className = metadata.getListResponseClassName();
        String source = buildClassSource(
                metadata.getResponsePackageName(),
                className,
                className + "，适合列表页展示。",
                metadata.getFields(),
                FieldMetadata::isGenerateForListResponse,
                true
        );
        writeJavaFile(metadata.getResponsePackageName(), className, source);
    }
}