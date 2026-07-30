package io.github.forgottenlab.emg.processor;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoViewProcessorValidationTest {

    @Test
    void invalidGroupProducesProcessorDiagnostic(@TempDir Path tempDir) {
        String[] invalidAutoViewGroups = {"", "   ", "user-name", "detail.name", "class"};
        for (int index = 0; index < invalidAutoViewGroups.length; index++) {
            String group = invalidAutoViewGroups[index];
            JavaFileObject entity = source("test.emg.v2.invalid.group.auto.Entity" + index, """
                    package test.emg.v2.invalid.group.auto;

                    import io.github.forgottenlab.emg.annotations.AutoView;
                    import io.github.forgottenlab.emg.annotations.ViewGroups;

                    @AutoView("%s")
                    public class Entity%d {
                        @ViewGroups("basic")
                        private Long id;
                    }
                    """.formatted(group, index));
            assertDiskFailure(
                    compileOnDisk(tempDir.resolve("auto-" + index), entity),
                    "@AutoView.value() 必须是合法的 Java 标识符且不能是关键字"
            );
        }

        String[] invalidFieldGroups = {"", "   ", "field-name", "field.name", "public"};
        for (int index = 0; index < invalidFieldGroups.length; index++) {
            String group = invalidFieldGroups[index];
            JavaFileObject entity = source("test.emg.v2.invalid.group.field.Entity" + index, """
                    package test.emg.v2.invalid.group.field;

                    import io.github.forgottenlab.emg.annotations.AutoView;
                    import io.github.forgottenlab.emg.annotations.ViewGroups;

                    @AutoView("basic")
                    public class Entity%d {
                        @ViewGroups("basic")
                        private Long id;
                        @ViewGroups("%s")
                        private String invalid;
                    }
                    """.formatted(index, group));
            assertDiskFailure(
                    compileOnDisk(tempDir.resolve("field-" + index), entity),
                    "@ViewGroups.value() 必须是合法的 Java 标识符且不能是关键字"
            );
        }
    }

    @Test
    void whitespaceAroundGroupProducesProcessorDiagnostic(@TempDir Path tempDir) {
        for (String group : List.of(" basic", "basic ", " basic ")) {
            String suffix = Integer.toHexString(group.hashCode()).replace('-', 'x');
            JavaFileObject entity = source("test.emg.v2.invalid.whitespace.Entity" + suffix, """
                    package test.emg.v2.invalid.whitespace;

                    import io.github.forgottenlab.emg.annotations.AutoView;
                    import io.github.forgottenlab.emg.annotations.ViewGroups;

                    @AutoView("%s")
                    public class Entity%s {
                        @ViewGroups("%s")
                        private Long id;
                    }
                    """.formatted(group, suffix, group));
            assertDiskFailure(
                    compileOnDisk(tempDir.resolve("group-" + suffix), entity),
                    "@AutoView.value() 必须是合法的 Java 标识符且不能是关键字"
            );
        }
    }

    @Test
    void invalidCustomNameProducesProcessorDiagnostic(@TempDir Path tempDir) {
        String[] names = {" ", "1View", "user-view", "class", "sample.model.UserView"};
        for (int index = 0; index < names.length; index++) {
            JavaFileObject entity = source("test.emg.v2.invalid.name.Entity" + index, """
                    package test.emg.v2.invalid.name;

                    import io.github.forgottenlab.emg.annotations.AutoView;
                    import io.github.forgottenlab.emg.annotations.ViewGroups;

                    @AutoView(value = "basic", name = "%s")
                    public class Entity%d {
                        @ViewGroups("basic")
                        private Long id;
                    }
                    """.formatted(names[index], index));
            assertDiskFailure(
                    compileOnDisk(tempDir.resolve("name-" + index), entity),
                    "@AutoView.name() 必须是合法的 Java 标识符且不能是关键字"
            );
        }
    }

    @Test
    void missingGroupProducesProcessorDiagnostic(@TempDir Path tempDir) {
        JavaFileObject entity = source("test.emg.v2.invalid.missing.entity.UserEntity", """
                package test.emg.v2.invalid.missing.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("detail")
                    private Long id;
                }
                """);

        assertDiskFailure(
                compileOnDisk(tempDir, entity),
                "@AutoView 引用的字段分组不存在或不包含任何字段: basic"
        );
    }

    @Test
    void duplicateViewTargetProducesProcessorDiagnostic(@TempDir Path tempDir) {
        JavaFileObject repeatedGroup = source("test.emg.v2.invalid.duplicate.entity.UserEntity", """
                package test.emg.v2.invalid.duplicate.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """);
        assertDiskFailure(
                compileOnDisk(tempDir.resolve("same-group"), repeatedGroup),
                "View 目标类型全限定名冲突"
        );

        JavaFileObject repeatedName = source("test.emg.v2.invalid.duplicatename.entity.UserEntity", """
                package test.emg.v2.invalid.duplicatename.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView(value = "basic", name = "SharedView")
                @AutoView(value = "detail", name = "SharedView")
                public class UserEntity {
                    @ViewGroups("basic") private Long id;
                    @ViewGroups("detail") private String name;
                }
                """);
        assertDiskFailure(
                compileOnDisk(tempDir.resolve("same-name"), repeatedName),
                "View 目标类型全限定名冲突"
        );
    }

    @Test
    void existingSourceTypeConflictProducesProcessorDiagnostic(@TempDir Path tempDir) {
        JavaFileObject entity = source("test.emg.v2.invalid.existing.entity.UserEntity", """
                package test.emg.v2.invalid.existing.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """);
        JavaFileObject existing = source("test.emg.v2.invalid.existing.model.view.UserBasicView", """
                package test.emg.v2.invalid.existing.model.view;
                public class UserBasicView { }
                """);

        assertDiskFailure(
                compileOnDisk(tempDir, entity, existing),
                "View 目标类型已存在: test.emg.v2.invalid.existing.model.view.UserBasicView"
        );
    }

    @Test
    void invalidViewDefinitionWritesNoPartialSources(@TempDir Path tempDir) {
        JavaFileObject combined = source("test.emg.v2.invalid.atomic.valid.entity.ValidEntity", """
                package test.emg.v2.invalid.atomic.valid.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoModel("Valid")
                @AutoView("basic")
                public class ValidEntity {
                    @ViewGroups("basic")
                    private Long id;
                    public Long getId() { return id; }
                }
                """);
        JavaFileObject invalid = source("test.emg.v2.invalid.atomic.bad.entity.BadEntity", """
                package test.emg.v2.invalid.atomic.bad.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;

                @AutoView("missing")
                public class BadEntity {
                    private String value;
                }
                """);

        assertDiskFailure(
                compileOnDisk(tempDir, combined, invalid),
                "@AutoView 引用的字段分组不存在或不包含任何字段: missing"
        );
    }

    @Test
    void autoViewRejectsNonClassTypes(@TempDir Path tempDir) {
        JavaFileObject annotatedInterface = source("test.emg.v2.invalid.kind.BadInterface", """
                package test.emg.v2.invalid.kind;
                import io.github.forgottenlab.emg.annotations.AutoView;
                @AutoView("basic")
                public interface BadInterface { }
                """);
        JavaFileObject annotatedEnum = source("test.emg.v2.invalid.kind.BadEnum", """
                package test.emg.v2.invalid.kind;
                import io.github.forgottenlab.emg.annotations.AutoView;
                @AutoView("basic")
                public enum BadEnum { VALUE }
                """);

        assertDiskFailure(
                compileOnDisk(tempDir.resolve("interface"), annotatedInterface),
                "@AutoView 只能标注在 class 上"
        );
        assertDiskFailure(
                compileOnDisk(tempDir.resolve("enum"), annotatedEnum),
                "@AutoView 只能标注在 class 上"
        );
    }

    @Test
    void differentEntitiesGeneratingSameViewTargetFail(@TempDir Path tempDir) {
        JavaFileObject first = source("test.emg.v2.invalid.cross.entity.FirstEntity", """
                package test.emg.v2.invalid.cross.entity;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                @AutoView(value = "basic", name = "SharedView")
                public class FirstEntity {
                    @ViewGroups("basic") private Long id;
                }
                """);
        JavaFileObject second = source("test.emg.v2.invalid.cross.entity.SecondEntity", """
                package test.emg.v2.invalid.cross.entity;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                @AutoView(value = "basic", name = "SharedView")
                public class SecondEntity {
                    @ViewGroups("basic") private String name;
                }
                """);

        DiskCompilation compilation = compileOnDisk(tempDir, first, second);
        assertDiskFailure(compilation, "View 目标类型全限定名冲突");
        assertDiagnosticContains(compilation, "FirstEntity");
        assertDiagnosticContains(compilation, "SecondEntity");
    }

    @Test
    void unsupportedViewFieldTypeProducesProcessorDiagnostic(@TempDir Path tempDir) {
        JavaFileObject entity = source("test.emg.v2.invalid.type.entity.GenericEntity", """
                package test.emg.v2.invalid.type.entity;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                @AutoView("basic")
                public class GenericEntity<T> {
                    @ViewGroups("basic") private T value;
                }
                """);

        assertDiskFailure(compileOnDisk(tempDir, entity), "不支持的字段类型 TYPEVAR");
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

    private record DiskCompilation(boolean successful, List<String> diagnostics, Path generatedSources) {
    }
}
