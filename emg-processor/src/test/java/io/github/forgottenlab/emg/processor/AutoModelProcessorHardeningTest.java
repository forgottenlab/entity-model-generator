package io.github.forgottenlab.emg.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoModelProcessorHardeningTest {

    @Test
    void supportsPrimitiveArrayGenericWildcardAndNestedTypes() {
        JavaFileObject nestedType = source("test.emg.hardening.types.ExternalTypes", """
                package test.emg.hardening.types;

                public final class ExternalTypes {
                    private ExternalTypes() {
                    }

                    public static final class Nested {
                    }
                }
                """);
        JavaFileObject entity = source("test.emg.hardening.types.entity.TypeMatrixEntity", """
                package test.emg.hardening.types.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;
                import test.emg.hardening.types.ExternalTypes;

                @AutoModel(value = "TypeMatrix", generateConverter = false)
                public class TypeMatrixEntity {
                    private byte byteValue;
                    private short shortValue;
                    private int intValue;
                    private long longValue;
                    private char charValue;
                    private float floatValue;
                    private double doubleValue;
                    private boolean booleanValue;
                    private String text;
                    private Integer integerValue;
                    private Boolean booleanObject;
                    private java.time.LocalDateTime createdAt;
                    private java.math.BigDecimal amount;
                    private String[] labels;
                    private byte[] payload;
                    private java.time.LocalDateTime[] timestamps;
                    private int[][] coordinates;
                    private java.util.List<String> names;
                    private java.util.List<java.time.LocalDateTime> eventTimes;
                    private java.util.Map<String, Integer> counts;
                    private java.util.Map<String, java.util.List<Long>> groupedIds;
                    private java.util.List<?> anyValues;
                    private java.util.List<? extends Number> upperBounds;
                    private java.util.List<? super Integer> lowerBounds;
                    private ExternalTypes.Nested nested;
                }
                """);

        Compilation compilation = compile(nestedType, entity);

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.hardening.types.model.dto.TypeMatrixDTO");
        assertContains(dto,
                "private byte byteValue;",
                "private short shortValue;",
                "private int intValue;",
                "private long longValue;",
                "private char charValue;",
                "private float floatValue;",
                "private double doubleValue;",
                "private boolean booleanValue;",
                "private String text;",
                "private Integer integerValue;",
                "private Boolean booleanObject;",
                "private LocalDateTime createdAt;",
                "private BigDecimal amount;",
                "private String[] labels;",
                "private byte[] payload;",
                "private LocalDateTime[] timestamps;",
                "private int[][] coordinates;",
                "private List<String> names;",
                "private List<LocalDateTime> eventTimes;",
                "private Map<String, Integer> counts;",
                "private Map<String, List<Long>> groupedIds;",
                "private List<?> anyValues;",
                "private List<? extends Number> upperBounds;",
                "private List<? super Integer> lowerBounds;",
                "private ExternalTypes.Nested nested;",
                "import java.util.List;",
                "import java.util.Map;",
                "import java.time.LocalDateTime;",
                "import java.math.BigDecimal;",
                "import test.emg.hardening.types.ExternalTypes;");
        assertNotContains(dto, "import java.lang.", "import java.util.List<");
    }

    @Test
    void usesQualifiedNamesWhenDeclaredTypesHaveTheSameSimpleName() {
        JavaFileObject firstType = source("test.emg.hardening.collision.alpha.Shared", """
                package test.emg.hardening.collision.alpha;

                public final class Shared {
                }
                """);
        JavaFileObject secondType = source("test.emg.hardening.collision.beta.Shared", """
                package test.emg.hardening.collision.beta;

                public final class Shared {
                }
                """);
        JavaFileObject entity = source("test.emg.hardening.collision.entity.CollisionEntity", """
                package test.emg.hardening.collision.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "Collision", generateConverter = false)
                public class CollisionEntity {
                    private test.emg.hardening.collision.alpha.Shared first;
                    private test.emg.hardening.collision.beta.Shared second;
                }
                """);

        Compilation compilation = compile(firstType, secondType, entity);

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.hardening.collision.model.dto.CollisionDTO");
        assertContains(dto,
                "private test.emg.hardening.collision.alpha.Shared first;",
                "private test.emg.hardening.collision.beta.Shared second;");
        assertNotContains(dto,
                "import test.emg.hardening.collision.alpha.Shared;",
                "import test.emg.hardening.collision.beta.Shared;");
    }

    @Test
    void rejectsTypeVariablesWithProcessorDiagnostic() {
        JavaFileObject entity = source("test.emg.hardening.unsupported.entity.GenericEntity", """
                package test.emg.hardening.unsupported.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "Generic", generateConverter = false)
                public class GenericEntity<T> {
                    private T value;
                }
                """);

        Compilation compilation = compile(entity);

        assertFailedWithError(compilation, "不支持的字段类型 TYPEVAR");
    }

    @Test
    void rejectsErrorTypesWithProcessorDiagnostic() {
        JavaFileObject entity = source("test.emg.hardening.unsupported.entity.ErrorTypeEntity", """
                package test.emg.hardening.unsupported.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "ErrorType", generateConverter = false)
                public class ErrorTypeEntity {
                    private MissingType value;
                }
                """);

        Compilation compilation = compile(entity);

        assertFailedWithError(compilation, "不支持的字段类型 ERROR");
    }

    @Test
    void excludesStaticFieldsButKeepsSupportedInstanceFieldModifiers() {
        JavaFileObject entity = source("test.emg.hardening.fields.entity.FieldBoundaryEntity", """
                package test.emg.hardening.fields.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("FieldBoundary")
                public class FieldBoundaryEntity {
                    private static String globalValue;
                    private String privateValue;
                    protected String protectedValue;
                    String packageValue;
                    public String publicValue;
                    private final String finalValue = "final";
                    private transient String transientValue;
                    private volatile String volatileValue;

                    public static String getGlobalValue() { return globalValue; }
                    public String getPrivateValue() { return privateValue; }
                    public String getProtectedValue() { return protectedValue; }
                    public String getPackageValue() { return packageValue; }
                    public String getPublicValue() { return publicValue; }
                    public String getFinalValue() { return finalValue; }
                    public String getTransientValue() { return transientValue; }
                    public String getVolatileValue() { return volatileValue; }
                }
                """);

        Compilation compilation = compile(entity);

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.hardening.fields.model.dto.FieldBoundaryDTO");
        String converter = generatedSource(compilation, "test.emg.hardening.fields.converter.FieldBoundaryConverter");
        assertContains(dto,
                "private String privateValue;",
                "private String protectedValue;",
                "private String packageValue;",
                "private String publicValue;",
                "private String finalValue;",
                "private String transientValue;",
                "private String volatileValue;");
        assertContains(converter,
                "source.getPrivateValue()",
                "source.getProtectedValue()",
                "source.getPackageValue()",
                "source.getPublicValue()",
                "source.getFinalValue()",
                "source.getTransientValue()",
                "source.getVolatileValue()");
        assertNotContains(dto, "globalValue", "GlobalValue");
        assertNotContains(converter, "globalValue", "GlobalValue");
    }

    @Test
    void doesNotRepeatInheritedFields() {
        JavaFileObject parent = source("test.emg.hardening.inheritance.entity.ParentEntity", """
                package test.emg.hardening.inheritance.entity;

                public class ParentEntity {
                    protected String inheritedValue;

                    public String getInheritedValue() {
                        return inheritedValue;
                    }
                }
                """);
        JavaFileObject child = source("test.emg.hardening.inheritance.entity.ChildEntity", """
                package test.emg.hardening.inheritance.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("Child")
                public class ChildEntity extends ParentEntity {
                    private String directValue;

                    public String getDirectValue() {
                        return directValue;
                    }
                }
                """);

        Compilation compilation = compile(parent, child);

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.hardening.inheritance.model.dto.ChildDTO");
        String converter = generatedSource(compilation, "test.emg.hardening.inheritance.converter.ChildConverter");
        assertContains(dto, "private String directValue;");
        assertContains(converter, "source.getDirectValue()");
        assertNotContains(dto, "inheritedValue", "InheritedValue");
        assertNotContains(converter, "inheritedValue", "InheritedValue");
    }

    @Test
    void acceptsLowercaseAndUnicodeJavaIdentifiers() {
        JavaFileObject lowercase = source("test.emg.hardening.names.lower.entity.LowercaseEntity", """
                package test.emg.hardening.names.lower.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "lowercase", generateConverter = false)
                public class LowercaseEntity {
                    private String value;
                }
                """);
        JavaFileObject unicode = source("test.emg.hardening.names.unicode.entity.UnicodeEntity", """
                package test.emg.hardening.names.unicode.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;
                import io.github.forgottenlab.emg.annotations.ResponseAlias;

                @AutoModel(value = "用户", generateConverter = false)
                public class UnicodeEntity {
                    @ResponseAlias("显示名")
                    private String name;
                }
                """);

        Compilation compilation = compile(lowercase, unicode);

        assertSucceeded(compilation);
        String lowercaseDto = generatedSource(
                compilation, "test.emg.hardening.names.lower.model.dto.lowercaseDTO"
        );
        String unicodeResponse = generatedSource(
                compilation, "test.emg.hardening.names.unicode.model.response.用户BaseResponse"
        );
        assertContains(lowercaseDto, "public class lowercaseDTO");
        assertContains(unicodeResponse, "public class 用户BaseResponse", "private String 显示名;");
    }

    @Test
    void rejectsEmptyInvalidAndKeywordModelNamesWithoutGeneratingSources(@TempDir Path tempDir) {
        String[] invalidNames = {"", "   ", "1User", "user-name", "class", "public"};
        for (int index = 0; index < invalidNames.length; index++) {
            String invalidName = invalidNames[index];
            JavaFileObject entity = source("test.emg.hardening.names.invalid.model.Entity" + index, """
                    package test.emg.hardening.names.invalid.model;

                    import io.github.forgottenlab.emg.annotations.AutoModel;

                    @AutoModel(value = "%s", generateConverter = false)
                    public class Entity%d {
                        private String value;
                    }
                    """.formatted(invalidName, index));

            DiskCompilation compilation = compileOnDisk(tempDir.resolve("model-" + index), entity);

            assertDiskFailure(compilation, "@AutoModel.value() 必须是合法的 Java 标识符且不能是关键字");
        }
    }

    @Test
    void rejectsEmptyInvalidAndKeywordResponseAliasesWithoutGeneratingSources(@TempDir Path tempDir) {
        String[] invalidAliases = {"", "   ", "1alias", "alias-name", "class", "public"};
        for (int index = 0; index < invalidAliases.length; index++) {
            String invalidAlias = invalidAliases[index];
            JavaFileObject entity = source("test.emg.hardening.names.invalid.alias.Entity" + index, """
                    package test.emg.hardening.names.invalid.alias;

                    import io.github.forgottenlab.emg.annotations.AutoModel;
                    import io.github.forgottenlab.emg.annotations.ResponseAlias;

                    @AutoModel(value = "Alias%d", generateConverter = false)
                    public class Entity%d {
                        @ResponseAlias("%s")
                        private String value;
                    }
                    """.formatted(index, index, invalidAlias));

            DiskCompilation compilation = compileOnDisk(tempDir.resolve("alias-" + index), entity);

            assertDiskFailure(compilation, "@ResponseAlias.value() 必须是合法的 Java 标识符且不能是关键字");
        }
    }

    @Test
    void rejectsInterfacesEnumsAndAnnotationTypesWithoutGeneratingSources(@TempDir Path tempDir) {
        JavaFileObject annotatedInterface = source("test.emg.hardening.kind.BadInterface", """
                package test.emg.hardening.kind;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("BadInterface")
                public interface BadInterface {
                }
                """);
        JavaFileObject annotatedEnum = source("test.emg.hardening.kind.BadEnum", """
                package test.emg.hardening.kind;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("BadEnum")
                public enum BadEnum {
                    VALUE
                }
                """);
        JavaFileObject annotatedAnnotation = source("test.emg.hardening.kind.BadAnnotation", """
                package test.emg.hardening.kind;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("BadAnnotation")
                public @interface BadAnnotation {
                }
                """);

        int index = 0;
        for (JavaFileObject invalidSource : new JavaFileObject[]{
                annotatedInterface, annotatedEnum, annotatedAnnotation
        }) {
            DiskCompilation compilation = compileOnDisk(tempDir.resolve("kind-" + index++), invalidSource);
            assertDiskFailure(compilation, "@AutoModel 只能标注在 class 上");
        }
    }

    @Test
    void rejectsDuplicateTargetQualifiedNamesBeforeWritingAnySources(@TempDir Path tempDir) {
        JavaFileObject first = source("test.emg.hardening.duplicate.entity.FirstEntity", """
                package test.emg.hardening.duplicate.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "Duplicate", generateConverter = false)
                public class FirstEntity {
                    private String firstValue;
                }
                """);
        JavaFileObject second = source("test.emg.hardening.duplicate.entity.SecondEntity", """
                package test.emg.hardening.duplicate.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel(value = "Duplicate", generateConverter = false)
                public class SecondEntity {
                    private String secondValue;
                }
                """);

        DiskCompilation compilation = compileOnDisk(tempDir, first, second);

        assertDiskFailure(compilation, "目标类型全限定名冲突");
        assertDiagnosticContains(compilation, "FirstEntity");
        assertDiagnosticContains(compilation, "SecondEntity");
    }

    @Test
    void usesPrimitiveBooleanAndWrapperBooleanGetterConventions() {
        JavaFileObject entity = source("test.emg.hardening.getter.entity.BooleanGetterEntity", """
                package test.emg.hardening.getter.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("BooleanGetter")
                public class BooleanGetterEntity {
                    private boolean active;
                    private boolean visible;
                    private Boolean enabled;

                    public boolean isActive() { return active; }
                    public boolean getVisible() { return visible; }
                    public Boolean getEnabled() { return enabled; }
                }
                """);

        Compilation compilation = compile(entity);

        assertSucceeded(compilation);
        String converter = generatedSource(
                compilation, "test.emg.hardening.getter.converter.BooleanGetterConverter"
        );
        assertContains(converter,
                "source.isActive()",
                "source.getVisible()",
                "source.getEnabled()");
        assertNotContains(converter, "source.getActive()", "source.isEnabled()");
    }

    @Test
    void prefersIsGetterForPrimitiveBooleanWhenBothGettersExist() {
        JavaFileObject entity = source("test.emg.hardening.getter.preference.entity.PreferenceEntity", """
                package test.emg.hardening.getter.preference.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("Preference")
                public class PreferenceEntity {
                    private boolean active;

                    public boolean isActive() { return active; }
                    public boolean getActive() { return active; }
                }
                """);

        Compilation compilation = compile(entity);

        assertSucceeded(compilation);
        String converter = generatedSource(
                compilation, "test.emg.hardening.getter.preference.converter.PreferenceConverter"
        );
        assertContains(converter, "source.isActive()");
        assertNotContains(converter, "source.getActive()");
    }

    @Test
    void rejectsFieldsWithoutCallableGetterBeforeWritingSources(@TempDir Path tempDir) {
        JavaFileObject entity = source("test.emg.hardening.getter.missing.entity.MissingGetterEntity", """
                package test.emg.hardening.getter.missing.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("MissingGetter")
                public class MissingGetterEntity {
                    private String name;

                    private String getName() { return name; }
                }
                """);

        DiskCompilation compilation = compileOnDisk(tempDir, entity);

        assertDiskFailure(compilation, "Converter 无法读取字段 name");
        assertDiagnosticContains(compilation, "public 实例零参 getter getName()");
    }

    @Test
    void doesNotTreatIsGetterAsWrapperBooleanGetter(@TempDir Path tempDir) {
        JavaFileObject entity = source("test.emg.hardening.getter.wrapper.entity.WrapperGetterEntity", """
                package test.emg.hardening.getter.wrapper.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("WrapperGetter")
                public class WrapperGetterEntity {
                    private Boolean enabled;

                    public Boolean isEnabled() { return enabled; }
                }
                """);

        DiskCompilation compilation = compileOnDisk(tempDir, entity);

        assertDiskFailure(compilation, "Converter 无法读取字段 enabled");
        assertDiagnosticContains(compilation, "getter getEnabled()");
    }

    private static Compilation compile(JavaFileObject... sources) {
        return Compiler.javac()
                .withOptions("--release", "17")
                .withProcessors(new AutoModelProcessor())
                .compile(sources);
    }

    private static JavaFileObject source(String qualifiedName, String content) {
        return JavaFileObjects.forSourceString(qualifiedName, content);
    }

    private static DiskCompilation compileOnDisk(Path tempDir, JavaFileObject... sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new AssertionError("A full JDK with javac is required");
        }

        Path sourceDirectory = tempDir.resolve("src");
        Path classOutput = tempDir.resolve("classes");
        Path generatedSources = tempDir.resolve("generated");

        try {
            Files.createDirectories(sourceDirectory);
            Files.createDirectories(classOutput);
            Files.createDirectories(generatedSources);
            List<Path> sourceFiles = new ArrayList<>();
            for (JavaFileObject source : sources) {
                String relativeName = source.getName().replace('\\', '/');
                while (relativeName.startsWith("/")) {
                    relativeName = relativeName.substring(1);
                }
                Path sourceFile = sourceDirectory.resolve(relativeName);
                Files.createDirectories(sourceFile.getParent());
                Files.writeString(sourceFile, source.getCharContent(false), StandardCharsets.UTF_8);
                sourceFiles.add(sourceFile);
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                    diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
                Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
                String classpath = System.getProperty(
                        "surefire.test.class.path",
                        System.getProperty("java.class.path")
                );
                List<String> options = List.of(
                        "--release", "17",
                        "-classpath", classpath,
                        "-d", classOutput.toString(),
                        "-s", generatedSources.toString()
                );
                JavaCompiler.CompilationTask task = compiler.getTask(
                        null, fileManager, diagnostics, options, null, units
                );
                task.setProcessors(List.of(new AutoModelProcessor()));
                boolean successful = task.call();
                List<String> messages = diagnostics.getDiagnostics().stream()
                        .map(diagnostic -> diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT))
                        .toList();
                return new DiskCompilation(successful, messages, generatedSources);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String generatedSource(Compilation compilation, String qualifiedName) {
        JavaFileObject source = compilation.generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("Missing generated source: " + qualifiedName));
        try {
            return source.getCharContent(false).toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void assertSucceeded(Compilation compilation) {
        assertEquals(Compilation.Status.SUCCESS, compilation.status(), diagnostics(compilation));
    }

    private static void assertFailedWithError(Compilation compilation, String messageFragment) {
        assertEquals(Compilation.Status.FAILURE, compilation.status(), diagnostics(compilation));
        assertTrue(compilation.errors().stream()
                        .anyMatch(diagnostic -> diagnostic.getMessage(Locale.ROOT).contains(messageFragment)),
                () -> "Missing ERROR diagnostic containing: " + messageFragment
                        + System.lineSeparator() + diagnostics(compilation));
    }

    private static void assertDiskFailure(DiskCompilation compilation, String messageFragment) {
        assertFalse(compilation.successful(), String.join(System.lineSeparator(), compilation.diagnostics()));
        assertDiagnosticContains(compilation, messageFragment);
        assertFalse(containsJavaSource(compilation.generatedSources()),
                "Validation failure must not leave partially generated source files");
    }

    private static void assertDiagnosticContains(DiskCompilation compilation, String messageFragment) {
        assertTrue(compilation.diagnostics().stream().anyMatch(message -> message.contains(messageFragment)),
                () -> "Missing diagnostic containing: " + messageFragment
                        + System.lineSeparator() + String.join(System.lineSeparator(), compilation.diagnostics()));
    }

    private static boolean containsJavaSource(Path directory) {
        try (var files = Files.walk(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().endsWith(".java"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String diagnostics(Compilation compilation) {
        StringBuilder result = new StringBuilder();
        compilation.diagnostics().forEach(diagnostic -> result
                .append(diagnostic.getKind())
                .append(": ")
                .append(diagnostic.getMessage(Locale.ROOT))
                .append(System.lineSeparator()));
        return result.toString();
    }

    private static void assertContains(String source, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), () -> "Expected generated source to contain: " + fragment);
        }
    }

    private static void assertNotContains(String source, String... fragments) {
        for (String fragment : fragments) {
            assertFalse(source.contains(fragment), () -> "Expected generated source not to contain: " + fragment);
        }
    }

    private record DiskCompilation(boolean successful, List<String> diagnostics, Path generatedSources) {
    }
}
