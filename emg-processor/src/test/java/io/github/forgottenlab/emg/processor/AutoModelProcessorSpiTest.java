package io.github.forgottenlab.emg.processor;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoModelProcessorSpiTest {

    private static final String PROCESSOR_CLASS = AutoModelProcessor.class.getName();
    private static final String SERVICE_RESOURCE = "META-INF/services/" + Processor.class.getName();
    private static final String SUPPORTED_ANNOTATION = "io.github.forgottenlab.emg.annotations.AutoModel";

    @Test
    void serviceDescriptorContainsSingleForgottenLabProcessor() {
        ClassLoader classLoader = AutoModelProcessor.class.getClassLoader();
        List<URL> resources;
        try {
            resources = Collections.list(classLoader.getResources(SERVICE_RESOURCE));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        List<String> providers = new ArrayList<>();
        for (URL resource : resources) {
            providers.addAll(readProviderLines(resource));
        }

        assertEquals(List.of(PROCESSOR_CLASS), providers,
                "Processor SPI must contain exactly one registration across the test classpath");
        assertFalse(String.join(System.lineSeparator(), providers).contains(legacyFqn()));
    }

    @Test
    void serviceLoaderDiscoversExactlyOneAutoModelProcessor() {
        List<Processor> discovered = ServiceLoader
                .load(Processor.class, AutoModelProcessor.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(processor -> processor.getClass().getName().equals(PROCESSOR_CLASS))
                .toList();

        assertEquals(1, discovered.size());
        assertEquals(AutoModelProcessor.class, discovered.get(0).getClass());
    }

    @Test
    void supportedAnnotationTypesContainOnlyForgottenLabAutoModel() {
        Set<String> supportedTypes = new AutoModelProcessor().getSupportedAnnotationTypes();

        assertEquals(Set.of(SUPPORTED_ANNOTATION), supportedTypes);
        assertFalse(String.join(System.lineSeparator(), supportedTypes).contains(legacyFqn()));
    }

    private static List<String> readProviderLines(URL resource) {
        try (var input = resource.openStream()) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String legacyFqn() {
        return String.join(".", "io", "github", "heran", "emg");
    }
}
