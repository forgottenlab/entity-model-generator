package io.github.forgottenlab.emg.processor.support;

import io.github.forgottenlab.emg.core.model.AutoViewMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.util.NameUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiler-visible provenance plus a receipt in this compilation's CLASS_OUTPUT.
 * A marker shipped by a dependency alone does not grant local output ownership.
 * All output access goes through Filer locations, never filesystem-path inference.
 */
public final class ViewOutputProvenance {
    public static final String MARKER = "io.github.forgottenlab.emg.annotations.internal.EmgGenerated";
    private static final String GENERATOR = "EMG";
    private static final String FORMAT = "EMG-VIEW-1";
    private final Filer filer;
    private final Elements elements;

    public enum ExistingTarget { ABSENT, EXTERNAL, HISTORICAL_EMG }

    public ViewOutputProvenance(ProcessingEnvironment environment) {
        filer = environment.getFiler();
        elements = environment.getElementUtils();
    }

    public String annotation(AutoViewMetadata view) {
        return "@" + MARKER + "(generator = \"EMG\", source = \"" + view.getSourceQualifiedName()
                + "\", identity = \"" + identity(view) + "\", schema = \"" + schema(view) + "\")\n";
    }

    public ExistingTarget classify(TypeElement existing, AutoViewMetadata view) {
        if (existing == null) {
            return ExistingTarget.ABSENT;
        }
        Map<String, String> marker = markerValues(existing);
        if (!GENERATOR.equals(marker.get("generator"))
                || !view.getSourceQualifiedName().equals(marker.get("source"))
                || !identity(view).equals(marker.get("identity"))) {
            return ExistingTarget.EXTERNAL;
        }
        // Check the receipt against the historical marker, not the newly desired schema.
        // Otherwise a genuine stale output would be incorrectly labelled a user conflict.
        String historicalSchema = marker.get("schema");
        try {
            String receipt = filer.getResource(StandardLocation.CLASS_OUTPUT, "", receiptName(view))
                    .getCharContent(false).toString();
            return receipt.equals(receipt(view, historicalSchema))
                    ? ExistingTarget.HISTORICAL_EMG : ExistingTarget.EXTERNAL;
        } catch (IOException exception) {
            return ExistingTarget.EXTERNAL;
        }
    }

    public boolean matchesDesiredModel(TypeElement existing, AutoViewMetadata view) {
        if (!schema(view).equals(markerValues(existing).get("schema"))
                || existing.getKind() != ElementKind.CLASS
                || !existing.getModifiers().equals(Set.of(Modifier.PUBLIC))
                || !existing.getTypeParameters().isEmpty()
                || !existing.getInterfaces().isEmpty()
                || !existing.getSuperclass().toString().equals("java.lang.Object")) {
            return false;
        }
        List<String> actualFields = new ArrayList<>();
        Map<String, String> actualMethods = new LinkedHashMap<>();
        int constructors = 0;
        for (Element member : existing.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD) {
                if (!member.getModifiers().equals(Set.of(Modifier.PRIVATE))) {
                    return false;
                }
                actualFields.add(member.getSimpleName() + ":" + canonical(member.asType().toString()));
            } else if (member.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) member;
                if (!method.getModifiers().equals(Set.of(Modifier.PUBLIC))
                        || method.isVarArgs() || !method.getTypeParameters().isEmpty()
                        || !method.getThrownTypes().isEmpty()) {
                    return false;
                }
                String parameters = method.getParameters().stream()
                        .map(p -> canonical(p.asType().toString())).reduce("", (a, b) -> a + ";" + b);
                if (actualMethods.put(method.getSimpleName() + "(" + parameters + ")",
                        canonical(method.getReturnType().toString())) != null) {
                    return false;
                }
            } else if (member.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructor = (ExecutableElement) member;
                if (!constructor.getModifiers().equals(Set.of(Modifier.PUBLIC))
                        || !constructor.getParameters().isEmpty()
                        || !constructor.getThrownTypes().isEmpty()) {
                    return false;
                }
                constructors++;
            } else {
                return false;
            }
        }
        List<String> expectedFields = new ArrayList<>();
        Map<String, String> expectedMethods = new LinkedHashMap<>();
        for (FieldMetadata field : view.getFields()) {
            String name = field.getSourceFieldName();
            String type = canonical(field.getQualifiedTypeName());
            expectedFields.add(name + ":" + type);
            String suffix = NameUtils.capitalize(name);
            expectedMethods.put("get" + suffix + "()", type);
            expectedMethods.put("set" + suffix + "(;" + type + ")", "void");
        }
        return constructors == 1 && actualFields.equals(expectedFields) && actualMethods.equals(expectedMethods);
    }

    public void writeReceipt(AutoViewMetadata view, Element source) throws IOException {
        try (Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "", receiptName(view), source)
                .openWriter()) {
            writer.write(receipt(view, schema(view)));
        }
    }

    private Map<String, String> markerValues(TypeElement type) {
        Map<String, String> values = new LinkedHashMap<>();
        for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
            if (((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().contentEquals(MARKER)) {
                elements.getElementValuesWithDefaults(annotation).forEach(
                        (key, value) -> values.put(key.getSimpleName().toString(), String.valueOf(value.getValue())));
            }
        }
        return values;
    }

    private String identity(AutoViewMetadata view) {
        return "view:" + view.getGroup() + ":" + view.getViewQualifiedName();
    }

    private String schema(AutoViewMetadata view) {
        StringBuilder contract = new StringBuilder(FORMAT).append('\n')
                .append(view.getSourceQualifiedName()).append('\n').append(identity(view)).append('\n');
        for (FieldMetadata field : view.getFields()) {
            contract.append(field.getSourceFieldName()).append(':')
                    .append(canonical(field.getQualifiedTypeName())).append('\n');
        }
        return hash(contract.toString());
    }

    private String receiptName(AutoViewMetadata view) {
        return "META-INF/emg/views/" + hash(view.getViewQualifiedName()) + ".owner";
    }

    private String receipt(AutoViewMetadata view, String schema) {
        return FORMAT + "\n" + GENERATOR + "\n" + view.getSourceQualifiedName() + "\n"
                + identity(view) + "\n" + schema + "\n";
    }

    private static String canonical(String type) {
        return type.replaceAll("\\s+", "");
    }

    private static String hash(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
