package io.github.forgottenlab.emg.annotations.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Internal compiler provenance, written only by EMG. Not a consumer configuration API.
 * CLASS retention allows inspection of retained bytecode without runtime reflection.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EmgGenerated {
    String generator();
    String source();
    String identity();
    String schema();
}
