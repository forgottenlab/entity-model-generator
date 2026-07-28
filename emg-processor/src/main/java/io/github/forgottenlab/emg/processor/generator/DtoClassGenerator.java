package io.github.forgottenlab.emg.processor.generator;


import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * DTO 生成器。
 *
 * <p>负责根据实体元数据生成：</p>
 * <ul>
 *     <li>{@code XxxDTO}</li>
 * </ul>
 *
 * <p>DTO 通常用于 service 层内部传输或作为中间结构对象。</p>
 */
public class DtoClassGenerator extends AbstractClassGenerator {

    public DtoClassGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    /**
     * 生成 DTO 源码。
     *
     * @param metadata 当前实体的生成元数据
     */
    public void generate(AutoModelMetadata metadata) {
        String className = metadata.getDtoClassName();
        String source = buildClassSource(
                metadata.getDtoPackageName(),
                className,
                className + "，适合 service 内部传输。",
                metadata.getFields(),
                FieldMetadata::isGenerateForDto,
                false
        );
        writeJavaFile(metadata.getDtoPackageName(), className, source);
    }
}