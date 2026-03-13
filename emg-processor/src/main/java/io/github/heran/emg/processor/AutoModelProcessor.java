package io.github.heran.emg.processor;

import io.github.heran.emg.annotation.AutoModel;
import io.github.heran.emg.core.model.AutoModelMetadata;
import io.github.heran.emg.processor.generator.BaseResponseClassGenerator;
import io.github.heran.emg.processor.generator.ConverterClassGenerator;
import io.github.heran.emg.processor.generator.DtoClassGenerator;
import io.github.heran.emg.processor.generator.ListResponseClassGenerator;
import io.github.heran.emg.processor.resolver.AutoModelMetadataResolver;
import io.github.heran.emg.processor.support.ProcessorLogger;
import io.github.heran.emg.processor.validator.AutoModelValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * AutoModel 的注解处理器入口。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>扫描所有带有 {@link AutoModel} 的实体类</li>
 *     <li>将实体类解析为统一的元数据对象 {@link AutoModelMetadata}</li>
 *     <li>执行编译期校验</li>
 *     <li>根据配置生成 DTO / BaseResponse / ListResponse / Converter</li>
 * </ul>
 *
 * <p>这是整个 AutoGen 编译期生成流程的总调度入口。</p>
 */
@SupportedAnnotationTypes("io.github.heran.emg.annotation.AutoModel")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AutoModelProcessor extends AbstractProcessor {

    /**
     * 元数据解析器。
     *
     * <p>负责把实体类及其字段解析成统一的元数据对象。</p>
     */
    private AutoModelMetadataResolver metadataResolver;

    /**
     * 编译期校验器。
     *
     * <p>负责检查注解配置是否合法，例如命名冲突、前缀非法等。</p>
     */
    private AutoModelValidator validator;

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

        this.metadataResolver = new AutoModelMetadataResolver(processingEnv);
        this.validator = new AutoModelValidator(processingEnv);
        this.dtoClassGenerator = new DtoClassGenerator(processingEnv);
        this.baseResponseClassGenerator = new BaseResponseClassGenerator(processingEnv);
        this.listResponseClassGenerator = new ListResponseClassGenerator(processingEnv);
        this.converterClassGenerator = new ConverterClassGenerator(processingEnv);
        this.logger = new ProcessorLogger(processingEnv.getMessager());
    }

    /**
     * 执行注解处理逻辑。
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>扫描所有 {@link AutoModel} 标注的类</li>
     *     <li>解析实体类元数据</li>
     *     <li>执行编译期校验</li>
     *     <li>根据开关生成目标源码</li>
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

        // 获取所有标注了 @AutoModel 的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(AutoModel.class);

        for (Element element : elements) {
            try {
                // 1. 解析实体类元数据
                AutoModelMetadata metadata = metadataResolver.resolve(element);

                // 2. 执行编译期校验
                validator.validate(metadata, element);

                logger.note("AutoGen 开始处理实体: " + metadata.getEntityQualifiedName());

                // 3. 按配置生成 DTO
                if (metadata.isGenerateDto()) {
                    dtoClassGenerator.generate(metadata);
                    logger.note("已生成 DTO: " + metadata.getDtoQualifiedName());
                }

                // 4. 按配置生成 BaseResponse
                if (metadata.isGenerateBaseResponse()) {
                    baseResponseClassGenerator.generate(metadata);
                    logger.note("已生成 BaseResponse: " + metadata.getBaseResponseQualifiedName());
                }

                // 5. 按配置生成 ListResponse
                if (metadata.isGenerateListResponse()) {
                    listResponseClassGenerator.generate(metadata);
                    logger.note("已生成 ListResponse: " + metadata.getListResponseQualifiedName());
                }

                // 6. 按配置生成 Converter
                if (metadata.isGenerateConverter()) {
                    converterClassGenerator.generate(metadata);
                    logger.note("已生成 Converter: " + metadata.getConverterQualifiedName());
                }

            } catch (Exception ex) {
                // 将异常转换为编译期错误，定位到当前源元素
                logger.error("AutoGen 处理失败: " + ex.getMessage(), element);
            }
        }

        return true;
    }
}