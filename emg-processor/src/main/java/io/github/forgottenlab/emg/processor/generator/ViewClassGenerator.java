package io.github.forgottenlab.emg.processor.generator;

import io.github.forgottenlab.emg.core.model.AutoViewMetadata;
import io.github.forgottenlab.emg.processor.support.ViewOutputProvenance;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;

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
        ViewOutputProvenance provenance = new ViewOutputProvenance(processingEnvironment);
        source = source.replace("public class " + metadata.getViewClassName() + " {",
                provenance.annotation(metadata) + "public class " + metadata.getViewClassName() + " {");
        writeJavaFile(metadata.getViewPackageName(), metadata.getViewClassName(), source);
        try {
            provenance.writeReceipt(metadata, processingEnvironment.getElementUtils()
                    .getTypeElement(metadata.getSourceQualifiedName()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot record EMG View output ownership: "
                    + metadata.getViewQualifiedName(), exception);
        }
    }
}
