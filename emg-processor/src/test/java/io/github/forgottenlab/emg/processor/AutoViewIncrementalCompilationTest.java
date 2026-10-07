package io.github.forgottenlab.emg.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A small consumer compiled by real javac on every invocation, retaining disk outputs. */
class AutoViewIncrementalCompilationTest {
    private static final String TARGET = "test.emg.incremental.model.view.ProductBasicView";
    private static final String RELATIVE_TARGET = TARGET.replace('.', '/') + ".java";

    @Test
    void firstGenerationPasses(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        Result result = compile(root, List.of(entity), "", new AutoModelProcessor());
        assertTrue(result.ok(), result.messages());
        assertTrue(Files.exists(root.resolve("generated/" + RELATIVE_TARGET)));
        assertTrue(Files.exists(root.resolve("classes/" + TARGET.replace('.', '/') + ".class")));
    }

    @Test
    void unchangedTouchedSourceWithRetainedOutputsForcesJavacAndPasses(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        Path view = root.resolve("generated/" + RELATIVE_TARGET);
        byte[] before = Files.readAllBytes(view);
        FileTime previous = Files.getLastModifiedTime(entity);
        Files.setLastModifiedTime(entity, FileTime.fromMillis(previous.toMillis() + 2000));
        assertTrue(Files.getLastModifiedTime(entity).compareTo(previous) > 0);
        CountingProcessor processor = new CountingProcessor();
        Result second = compile(root, List.of(entity, view), "", processor);
        assertTrue(processor.invocations > 0, "javac/APT must actually run");
        System.out.println("NON_CLEAN_JAVAC: " + second.messages());
        assertTrue(second.ok(), second.messages());
        assertArrayEquals(before, Files.readAllBytes(view));
    }

    @Test
    void userDefinedTargetStillFails(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        Path user = write(root.resolve("src/" + RELATIVE_TARGET), userTarget());
        Result result = compile(root, List.of(entity, user), "", new AutoModelProcessor());
        assertConflict(result);
    }

    @Test
    void dependencyDefinedTargetStillFails(@TempDir Path root) throws Exception {
        Path dependency = root.resolve("dependency");
        Path source = write(dependency.resolve("src/" + RELATIVE_TARGET), userTarget());
        assertTrue(compile(dependency, List.of(source), "", null).ok());
        Path entity = entity(root, fixture());
        Result result = compile(root, List.of(entity), dependency.resolve("classes").toString(),
                new AutoModelProcessor());
        assertConflict(result);
    }

    @Test
    void changedSchemaRetainedOutputFailsDeterministically(@TempDir Path root) throws Exception {
        for (String changed : changedSchemas()) {
            Path scenario = root.resolve("case-" + Integer.toHexString(changed.hashCode()));
            Path entity = entity(scenario, fixture());
            assertTrue(compile(scenario, List.of(entity), "", new AutoModelProcessor()).ok());
            Path view = scenario.resolve("generated/" + RELATIVE_TARGET);
            byte[] before = Files.readAllBytes(view);
            entity(scenario, changed);
            Result result = compile(scenario, List.of(entity, view), "", new AutoModelProcessor());
            assertFalse(result.ok());
            assertTrue(result.messages().contains("EMG generated type is stale; clean regeneration is required"),
                    result.messages());
            assertFalse(result.messages().contains("View 目标类型已存在"), result.messages());
            assertArrayEquals(before, Files.readAllBytes(view));
        }
    }

    @Test
    void freshCompileAfterSchemaChangeUpdatesModel(@TempDir Path root) throws Exception {
        // Separate clean output directories avoid deleting arbitrary outputs.
        Path first = root.resolve("before");
        assertTrue(compile(first, List.of(entity(first, fixture())), "", new AutoModelProcessor()).ok());
        List<String> changes = changedSchemas();
        for (int i = 0; i < changes.size(); i++) {
            Path fresh = root.resolve("after-clean-" + i);
            Result result = compile(fresh, List.of(entity(fresh, changes.get(i))), "", new AutoModelProcessor());
            assertTrue(result.ok(), result.messages());
            String view = Files.readString(fresh.resolve("generated/" + RELATIVE_TARGET));
            if (i == 0) {
                assertTrue(view.contains("getStock()"));
            } else if (i == 1) {
                assertFalse(view.contains("getName()"));
            } else {
                assertTrue(view.contains("public Integer getName()"));
            }
        }
    }

    @Test
    void filerCannotRecreateRetainedSourceInput(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        FilerProbe probe = new FilerProbe();
        Result result = compile(root, List.of(entity, root.resolve("generated/" + RELATIVE_TARGET)), "", probe);
        assertTrue(result.ok(), result.messages());
        assertNotNull(probe.failure);
        assertTrue(probe.failure instanceof javax.annotation.processing.FilerException, probe.failure.toString());
        System.out.println("FILER_SOURCE_INPUT: " + probe.failure);
    }

    @Test
    void classOnlyHistoryCanBeReused(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        CountingProcessor processor = new CountingProcessor();
        Result result = compile(root, List.of(entity), "", processor);
        assertTrue(processor.invocations > 0);
        assertTrue(result.ok(), result.messages());
        assertTrue(result.messages().contains("Reusing verified EMG View"), result.messages());
    }

    @Test
    void classOnlyStaleHistoryFails(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        entity(root, changedSchemas().get(0));
        Result result = compile(root, List.of(entity), "", new AutoModelProcessor());
        assertFalse(result.ok());
        assertTrue(result.messages().contains("EMG generated type is stale"), result.messages());
    }

    @Test
    void emgGeneratedDependencyWithoutLocalOwnershipStillConflicts(@TempDir Path root) throws Exception {
        Path dependency = root.resolve("dependency");
        assertTrue(compile(dependency, List.of(entity(dependency, fixture())), "", new AutoModelProcessor()).ok());
        Path consumer = root.resolve("consumer");
        Result result = compile(consumer, List.of(entity(consumer, fixture())),
                dependency.resolve("classes").toString(), new AutoModelProcessor());
        assertConflict(result);
    }

    @Test
    void copiedMarkerWithoutReceiptDoesNotGrantOwnership(@TempDir Path root) throws Exception {
        Path original = root.resolve("original");
        assertTrue(compile(original, List.of(entity(original, fixture())), "", new AutoModelProcessor()).ok());
        String markedSource = Files.readString(original.resolve("generated/" + RELATIVE_TARGET));
        Path consumer = root.resolve("consumer");
        Path copy = write(consumer.resolve("src/" + RELATIVE_TARGET), markedSource);
        assertConflict(compile(consumer, List.of(entity(consumer, fixture()), copy), "", new AutoModelProcessor()));
    }

    @Test
    void corruptedOwnershipReceiptDoesNotGrantReuse(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        try (var files = Files.list(root.resolve("classes/META-INF/emg/views"))) {
            Path receipt = files.findFirst().orElseThrow();
            write(receipt, "not an EMG ownership receipt");
        }
        assertConflict(compile(root, List.of(entity), "", new AutoModelProcessor()));
    }

    @Test
    void legacySourceGeneratedAnnotationCannotProveOwnership(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        String legacy = "package test.emg.incremental.model.view; "
                + "@javax.annotation.processing.Generated(\"EMG V2\") public class ProductBasicView {}";
        Path view = write(root.resolve("generated/" + RELATIVE_TARGET), legacy);
        Result result = compile(root, List.of(entity, view), "", new AutoModelProcessor());
        assertConflict(result);
        assertTrue(result.messages().contains("clean regeneration is required"), result.messages());
    }

    @Test
    void editedOutputStructureIsRejectedDespiteUnchangedMarker(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        Path view = root.resolve("generated/" + RELATIVE_TARGET);
        write(view, Files.readString(view).replace("private String name;", "private String name;\nprivate int extra;"));
        Result result = compile(root, List.of(entity, view), "", new AutoModelProcessor());
        assertFalse(result.ok());
        assertTrue(result.messages().contains("EMG generated type is stale"), result.messages());
    }

    @Test
    void historicalOutputOfAnotherEntityStillConflicts(@TempDir Path root) throws Exception {
        assertTrue(compile(root, List.of(entity(root, fixture())), "", new AutoModelProcessor()).ok());
        String other = fixture().replace("@AutoView(\"basic\")",
                "@AutoView(value = \"basic\", name = \"ProductBasicView\")")
                .replace("class ProductEntity", "class OtherEntity");
        Path entity = write(root.resolve("src/test/emg/incremental/entity/OtherEntity.java"), other);
        assertConflict(compile(root, List.of(entity), "", new AutoModelProcessor()));
    }

    @Test
    void multipleViewsAndComplexFieldsCanBeReused(@TempDir Path root) throws Exception {
        String content = fixture().replace("@AutoView(\"basic\")",
                "@AutoView(\"basic\")\n@AutoView(\"detail\")")
                .replace("@ViewGroups(\"basic\")", "@ViewGroups({\"basic\", \"detail\"})")
                .replace("private String name;", "private java.util.Map<String, java.util.List<? extends Number[]>> name;");
        Path entity = entity(root, content);
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        Path basic = root.resolve("generated/" + RELATIVE_TARGET);
        Path detail = root.resolve("generated/" + RELATIVE_TARGET.replace("Basic", "Detail"));
        Result result = compile(root, List.of(entity, basic, detail), "", new AutoModelProcessor());
        assertTrue(result.ok(), result.messages());
        Result classOnly = compile(root, List.of(entity), "", new AutoModelProcessor());
        assertTrue(classOnly.ok(), classOnly.messages());
    }

    @Test
    void filerCanRecreateWhenOnlyOldClassIsVisible(@TempDir Path root) throws Exception {
        Path entity = entity(root, fixture());
        assertTrue(compile(root, List.of(entity), "", new AutoModelProcessor()).ok());
        FilerProbe probe = new FilerProbe();
        Result result = compile(root, List.of(entity), "", probe);
        System.out.println("FILER_CLASS_ONLY: failure=" + probe.failure + result.messages());
        assertTrue(result.ok(), result.messages());
        assertNull(probe.failure);
        assertEquals(userTarget(), Files.readString(root.resolve("generated/" + RELATIVE_TARGET)));
    }

    private static void assertConflict(Result result) {
        assertFalse(result.ok(), result.messages());
        assertTrue(result.messages().contains("View 目标类型已存在: " + TARGET), result.messages());
    }

    private static List<String> changedSchemas() throws IOException {
        String original = fixture();
        return List.of(
                original.replace("private String name;", "private String name;\n"
                        + "    @ViewGroups(\"basic\") private Integer stock;"),
                original.replace("    @ViewGroups(\"basic\")\n    private String name;\n", ""),
                original.replace("private String name;", "private Integer name;"));
    }

    private static String userTarget() {
        return "package test.emg.incremental.model.view; public class ProductBasicView {}";
    }

    private static String fixture() throws IOException {
        try (var input = AutoViewIncrementalCompilationTest.class.getResourceAsStream(
                "/incremental/ProductEntity.java.txt")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static Path entity(Path root, String content) throws IOException {
        return write(root.resolve("src/test/emg/incremental/entity/ProductEntity.java"), content);
    }

    private static Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static Result compile(Path root, List<Path> sources, String extraClasspath,
                                  AbstractProcessor processor) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Files.createDirectories(root.resolve("classes"));
        Files.createDirectories(root.resolve("generated"));
        String classpath = root.resolve("classes") + File.pathSeparator + extraClasspath
                + File.pathSeparator + System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(List.of("--release", "17", "-classpath", classpath,
                    "-d", root.resolve("classes").toString(), "-s", root.resolve("generated").toString()));
            if (processor == null) {
                options.add("-proc:none");
            }
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, options,
                    null, manager.getJavaFileObjectsFromPaths(sources));
            if (processor != null) {
                task.setProcessors(List.of(processor));
            }
            boolean ok = task.call();
            String messages = diagnostics.getDiagnostics().stream()
                    .map(d -> d.getKind() + ": " + d.getMessage(Locale.ROOT))
                    .reduce("", (a, b) -> a + "\n" + b);
            return new Result(ok, messages);
        }
    }

    private record Result(boolean ok, String messages) {}

    private static final class CountingProcessor extends AutoModelProcessor {
        private int invocations;

        @Override public Set<String> getSupportedAnnotationTypes() {
            return Set.of("io.github.forgottenlab.emg.annotations.AutoView",
                    "io.github.forgottenlab.emg.annotations.AutoViews");
        }
        @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.RELEASE_17; }
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
            invocations++;
            return super.process(annotations, round);
        }
    }

    private static final class FilerProbe extends AbstractProcessor {
        private IOException failure;
        private boolean attempted;
        @Override public Set<String> getSupportedAnnotationTypes() { return Set.of("*"); }
        @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.RELEASE_17; }
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
            if (!attempted && !round.processingOver()) {
                attempted = true;
                try {
                    // Resolve the existing type just as a validator would before attempting generation.
                    assertNotNull(processingEnv.getElementUtils().getTypeElement(TARGET));
                    try (var writer = processingEnv.getFiler().createSourceFile(TARGET).openWriter()) {
                        writer.write(userTarget());
                    }
                } catch (IOException exception) {
                    failure = exception;
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, exception.toString());
                }
            }
            return false;
        }
    }
}
