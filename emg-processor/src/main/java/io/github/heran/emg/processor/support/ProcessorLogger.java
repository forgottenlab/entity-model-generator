package io.github.heran.emg.processor.support;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * APT 日志输出工具。
 *
 * <p>对 {@link Messager} 做轻量封装，统一输出 note / warning / error。</p>
 */
public class ProcessorLogger {

    /**
     * 编译期消息输出器。
     */
    private final Messager messager;

    public ProcessorLogger(Messager messager) {
        this.messager = messager;
    }

    /**
     * 输出普通提示信息。
     */
    public void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * 输出警告信息。
     */
    public void warn(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    /**
     * 输出错误信息，并绑定到具体源码元素。
     */
    public void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}