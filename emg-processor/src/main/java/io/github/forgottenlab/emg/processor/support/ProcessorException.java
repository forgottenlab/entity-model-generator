package io.github.forgottenlab.emg.processor.support;

import javax.lang.model.element.Element;

/**
 * 可定位到具体源码元素的处理器异常。
 */
public class ProcessorException extends RuntimeException {

    private final transient Element element;

    public ProcessorException(String message, Element element) {
        super(message);
        this.element = element;
    }

    public ProcessorException(String message, Element element, Throwable cause) {
        super(message, cause);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
