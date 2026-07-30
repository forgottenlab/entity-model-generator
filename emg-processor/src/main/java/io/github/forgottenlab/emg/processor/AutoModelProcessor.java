package io.github.forgottenlab.emg.processor;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.annotations.AutoViews;
import io.github.forgottenlab.emg.core.model.AutoModelMetadata;
import io.github.forgottenlab.emg.core.model.AutoViewMetadata;
import io.github.forgottenlab.emg.core.model.SourceMetadata;
import io.github.forgottenlab.emg.processor.generator.BaseResponseClassGenerator;
import io.github.forgottenlab.emg.processor.generator.ConverterClassGenerator;
import io.github.forgottenlab.emg.processor.generator.DtoClassGenerator;
import io.github.forgottenlab.emg.processor.generator.ListResponseClassGenerator;
import io.github.forgottenlab.emg.processor.generator.ViewClassGenerator;
import io.github.forgottenlab.emg.processor.resolver.AutoModelMetadataResolver;
import io.github.forgottenlab.emg.processor.resolver.AutoViewMetadataResolver;
import io.github.forgottenlab.emg.processor.resolver.SourceMetadataResolver;
import io.github.forgottenlab.emg.processor.support.ProcessorException;
import io.github.forgottenlab.emg.processor.support.ProcessorLogger;
import io.github.forgottenlab.emg.processor.validator.AutoModelValidator;
import io.github.forgottenlab.emg.processor.validator.AutoViewValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EMG V1 标准模型与 V2 自定义 View 的注解处理器入口。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>收集带有 {@link AutoModel} 或 {@link AutoView} 的来源类</li>
 *     <li>每个来源类型只解析一次字段元数据</li>
 *     <li>执行 V1/V2 整轮编译期预检</li>
 *     <li>统一生成标准模型和自定义 View</li>
 * </ul>
 *
 * <p>这是整个 AutoGen 编译期生成流程的总调度入口。</p>
 */
@SupportedAnnotationTypes({
        "io.github.forgottenlab.emg.annotations.AutoModel",
        "io.github.forgottenlab.emg.annotations.AutoView",
        "io.github.forgottenlab.emg.annotations.AutoViews",
        "io.github.forgottenlab.emg.annotations.ViewGroups"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AutoModelProcessor extends AbstractProcessor {

    /**
     * 元数据解析器。
     *
     * <p>负责把实体类及其字段解析成统一的元数据对象。</p>
     */
    private SourceMetadataResolver sourceMetadataResolver;

    private AutoModelMetadataResolver autoModelMetadataResolver;

    private AutoViewMetadataResolver autoViewMetadataResolver;

    /**
     * 编译期校验器。
     *
     * <p>负责检查注解配置是否合法，例如命名冲突、前缀非法等。</p>
     */
    private AutoModelValidator validator;

    private AutoViewValidator autoViewValidator;

    /**
     * DTO 生成器。
     */
    private DtoClassGenerator dtoClassGenerator;

    /**
     * BaseResponse 生成器。
     */
    private BaseResponseClassGenerator baseResponseClassGenerator;

    /**
     * ListResponse 生成器。
     */
    private ListResponseClassGenerator listResponseClassGenerator;

    /**
     * Converter 生成器。
     */
    private ConverterClassGenerator converterClassGenerator;

    /**
     * 自定义 View 生成器。
     */
    private ViewClassGenerator viewClassGenerator;

    /**
     * APT 日志输出工具。
     */
    private ProcessorLogger logger;

    /**
     * 初始化处理器。
     *
     * <p>该方法会在注解处理器启动时调用一次，用于初始化各类解析器、校验器和生成器。</p>
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.sourceMetadataResolver = new SourceMetadataResolver(processingEnv);
        this.autoModelMetadataResolver = new AutoModelMetadataResolver(processingEnv);
        this.autoViewMetadataResolver = new AutoViewMetadataResolver();
        this.validator = new AutoModelValidator(processingEnv);
        this.autoViewValidator = new AutoViewValidator(processingEnv);
        this.dtoClassGenerator = new DtoClassGenerator(processingEnv);
        this.baseResponseClassGenerator = new BaseResponseClassGenerator(processingEnv);
        this.listResponseClassGenerator = new ListResponseClassGenerator(processingEnv);
        this.converterClassGenerator = new ConverterClassGenerator(processingEnv);
        this.viewClassGenerator = new ViewClassGenerator(processingEnv);
        this.logger = new ProcessorLogger(processingEnv.getMessager());
    }

    /**
     * 执行注解处理逻辑。
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>收集 {@link AutoModel}、{@link AutoView}、{@link AutoViews} 来源并集</li>
     *     <li>一次解析来源字段并构建 V1/V2 元数据</li>
     *     <li>执行全部命名、分组和目标冲突校验</li>
     *     <li>预检全部通过后统一生成目标源码</li>
     * </ol>
     *
     * @param annotations 当前轮次中匹配到的注解类型
     * @param roundEnv 当前轮次环境
     * @return true 表示该注解已由当前处理器处理完成
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 最后一轮处理结束后，不再继续生成源码
        if (roundEnv.processingOver()) {
            return false;
        }

        // 收集 V1/V2 模型来源的并集；@ViewGroups 本身不触发生成。
        Set<Element> elements = new LinkedHashSet<>();
        elements.addAll(roundEnv.getElementsAnnotatedWith(AutoModel.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(AutoView.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(AutoViews.class));

        Map<AutoModelMetadata, Element> autoModelCandidates = new LinkedHashMap<>();
        Map<AutoViewMetadata, Element> autoViewCandidates = new LinkedHashMap<>();
        boolean validationFailed = false;

        // 第一阶段：解析并校验整轮输入，不写入任何源码。
        for (Element element : elements) {
            try {
                AutoModel autoModel = element.getAnnotation(AutoModel.class);
                List<AutoView> autoViews = List.of(element.getAnnotationsByType(AutoView.class));

                if (autoModel != null) {
                    validator.validateAnnotatedElement(element);
                }
                if (!autoViews.isEmpty()) {
                    autoViewValidator.validateAnnotatedElement(element);
                    autoViewValidator.validateAnnotations(autoViews, element);
                }

                TypeElement typeElement = (TypeElement) element;
                SourceMetadata sourceMetadata = sourceMetadataResolver.resolve(typeElement, autoModel);

                if (autoModel != null) {
                    AutoModelMetadata metadata = autoModelMetadataResolver.resolve(sourceMetadata, autoModel);
                    validator.validate(metadata, element);
                    autoModelCandidates.put(metadata, element);
                }

                if (!autoViews.isEmpty()) {
                    autoViewValidator.validateFieldGroups(sourceMetadata, element);
                    List<AutoViewMetadata> views = autoViewMetadataResolver.resolve(
                            sourceMetadata, autoModel, autoViews
                    );
                    autoViewValidator.validateViews(views, element);
                    for (AutoViewMetadata view : views) {
                        autoViewCandidates.put(view, element);
                    }
                }
            } catch (ProcessorException exception) {
                logger.error("AutoGen 处理失败: " + exception.getMessage(), exception.getElement());
                validationFailed = true;
            } catch (Exception exception) {
                logger.error("AutoGen 处理失败: " + exception.getMessage(), element);
                validationFailed = true;
            }
        }

        if (validationFailed) {
            return true;
        }

        try {
            validator.validateTargetTypeConflicts(autoModelCandidates);
            autoViewValidator.validateTargetTypeConflicts(autoViewCandidates);
        } catch (ProcessorException exception) {
            logger.error("AutoGen 处理失败: " + exception.getMessage(), exception.getElement());
            return true;
        }

        // 第二阶段：整轮预检通过后，才按配置生成源码。
        for (Map.Entry<AutoModelMetadata, Element> candidate : autoModelCandidates.entrySet()) {
            generate(candidate.getKey(), candidate.getValue());
        }
        for (Map.Entry<AutoViewMetadata, Element> candidate : autoViewCandidates.entrySet()) {
            generateView(candidate.getKey(), candidate.getValue());
        }

        return true;
    }

    private void generate(AutoModelMetadata metadata, Element sourceElement) {
        try {
            logger.note("AutoGen 开始处理实体: " + metadata.getEntityQualifiedName());

            if (metadata.isGenerateDto()) {
                dtoClassGenerator.generate(metadata);
                logger.note("已生成 DTO: " + metadata.getDtoQualifiedName());
            }
            if (metadata.isGenerateBaseResponse()) {
                baseResponseClassGenerator.generate(metadata);
                logger.note("已生成 BaseResponse: " + metadata.getBaseResponseQualifiedName());
            }
            if (metadata.isGenerateListResponse()) {
                listResponseClassGenerator.generate(metadata);
                logger.note("已生成 ListResponse: " + metadata.getListResponseQualifiedName());
            }
            if (metadata.isGenerateConverter()) {
                converterClassGenerator.generate(metadata);
                logger.note("已生成 Converter: " + metadata.getConverterQualifiedName());
            }
        } catch (Exception exception) {
            logger.error("AutoGen 处理失败: " + exception.getMessage(), sourceElement);
        }
    }

    private void generateView(AutoViewMetadata metadata, Element sourceElement) {
        try {
            viewClassGenerator.generate(metadata);
            logger.note("已生成 View: " + metadata.getViewQualifiedName());
        } catch (Exception exception) {
            logger.error("AutoView 处理失败: " + exception.getMessage(), sourceElement);
        }
    }
}
