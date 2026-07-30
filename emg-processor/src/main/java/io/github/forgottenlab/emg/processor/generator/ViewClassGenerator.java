package io.github.forgottenlab.emg.processor.generator;

import io.github.forgottenlab.emg.core.model.AutoViewMetadata;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * V2 自定义单表 View 生成器。
 */
public class ViewClassGenerator extends AbstractClassGenerator {

    public ViewClassGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    public void generate(AutoViewMetadata metadata) {
        String source = buildClassSource(
                metadata.getViewPackageName(),
                metadata.getViewClassName(),
                metadata.getViewClassName() + "，字段分组: " + metadata.getGroup() + "。",
                metadata.getFields(),
                ignored -> true,
                false,
                "EMG V2"
        );
        writeJavaFile(metadata.getViewPackageName(), metadata.getViewClassName(), source);
    }
}
