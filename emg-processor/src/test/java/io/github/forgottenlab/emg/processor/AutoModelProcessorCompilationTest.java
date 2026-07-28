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
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoModelProcessorCompilationTest {

    @Test
    void generatesAllModelsAndCompilesGeneratedSources() {
        JavaFileObject entity = entitySource(
                "test.emg.defaults.entity.UserEntity",
                "@AutoModel(\"User\")",
                field("Long", "id"),
                field("String", "username"),
                field("java.time.LocalDateTime", "createdAt")
        );

        Compilation compilation = compile(entity);

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.defaults.model.dto.UserDTO");
        assertGeneratedAndCompiled(compilation, "test.emg.defaults.model.response.UserBaseResponse");
        assertGeneratedAndCompiled(compilation, "test.emg.defaults.model.response.UserListResponse");
        assertGeneratedAndCompiled(compilation, "test.emg.defaults.converter.UserConverter");

        String dto = generatedSource(compilation, "test.emg.defaults.model.dto.UserDTO");
        assertContains(dto,
                "package test.emg.defaults.model.dto;",
                "public class UserDTO",
                "private Long id;",
                "private String username;",
                "private LocalDateTime createdAt;",
                "public Long getId()",
                "public void setId(Long id)",
                "public LocalDateTime getCreatedAt()",
                "public void setCreatedAt(LocalDateTime createdAt)");

        String converter = generatedSource(compilation, "test.emg.defaults.converter.UserConverter");
        assertContains(converter,
                "public static UserDTO toDto(UserEntity source)",
                "public static UserBaseResponse toBaseResponse(UserEntity source)",
                "public static UserListResponse toListResponse(UserEntity source)",
                "public static List<UserDTO> toDtoList(List<UserEntity> sourceList)",
                "public static List<UserBaseResponse> toBaseResponseList(List<UserEntity> sourceList)",
                "public static List<UserListResponse> toListResponseList(List<UserEntity> sourceList)",
                "if (source == null)",
                "if (sourceList == null)",
                "return null;");
    }

    @Test
    void dtoIgnoreOnlyExcludesDtoField() {
        Compilation compilation = compile(entitySource(
                "test.emg.ignore.dto.entity.AccountEntity",
                "@AutoModel(\"Account\")",
                field("String", "publicName"),
                field("String", "internalCode", "@DtoIgnore")
        ));

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.ignore.dto.model.dto.AccountDTO");
        String base = generatedSource(compilation, "test.emg.ignore.dto.model.response.AccountBaseResponse");
        String list = generatedSource(compilation, "test.emg.ignore.dto.model.response.AccountListResponse");

        assertNotContains(dto, "internalCode");
        assertContains(base, "private String internalCode;", "getInternalCode()", "setInternalCode(String internalCode)");
        assertContains(list, "private String internalCode;", "getInternalCode()", "setInternalCode(String internalCode)");
    }

    @Test
    void responseIgnoreExcludesFieldFromBothResponses() {
        Compilation compilation = compile(entitySource(
                "test.emg.ignore.response.entity.AccountEntity",
                "@AutoModel(\"Account\")",
                field("String", "publicName"),
                field("String", "secret", "@ResponseIgnore")
        ));

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.ignore.response.model.dto.AccountDTO");
        String base = generatedSource(compilation, "test.emg.ignore.response.model.response.AccountBaseResponse");
        String list = generatedSource(compilation, "test.emg.ignore.response.model.response.AccountListResponse");

        assertContains(dto, "private String secret;", "getSecret()", "setSecret(String secret)");
        assertNotContains(base, "secret");
        assertNotContains(list, "secret");
    }

    @Test
    void listIgnoreKeepsBaseResponseButExcludesListResponse() {
        Compilation compilation = compile(entitySource(
                "test.emg.ignore.list.entity.ArticleEntity",
                "@AutoModel(\"Article\")",
                field("String", "title"),
                field("String", "details", "@ListIgnore")
        ));

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.ignore.list.model.dto.ArticleDTO");
        String base = generatedSource(compilation, "test.emg.ignore.list.model.response.ArticleBaseResponse");
        String list = generatedSource(compilation, "test.emg.ignore.list.model.response.ArticleListResponse");

        assertContains(dto, "private String details;");
        assertContains(base, "private String details;", "getDetails()", "setDetails(String details)");
        assertNotContains(list, "details");
    }

    @Test
    void responseAliasRenamesResponsesAndConverterMapping() {
        Compilation compilation = compile(entitySource(
                "test.emg.alias.entity.ProfileEntity",
                "@AutoModel(\"Profile\")",
                field("Long", "id"),
                field("String", "nickname", "@ResponseAlias(\"displayName\")")
        ));

        assertSucceeded(compilation);
        String dto = generatedSource(compilation, "test.emg.alias.model.dto.ProfileDTO");
        String base = generatedSource(compilation, "test.emg.alias.model.response.ProfileBaseResponse");
        String list = generatedSource(compilation, "test.emg.alias.model.response.ProfileListResponse");
        String converter = generatedSource(compilation, "test.emg.alias.converter.ProfileConverter");

        assertContains(dto, "private String nickname;", "getNickname()", "setNickname(String nickname)");
        assertNotContains(dto, "displayName");
        assertContains(base, "private String displayName;", "getDisplayName()", "setDisplayName(String displayName)");
        assertContains(list, "private String displayName;", "getDisplayName()", "setDisplayName(String displayName)");
        assertNotContains(base, "nickname");
        assertNotContains(list, "nickname");
        assertContains(converter,
                "target.setDisplayName(source.getNickname());",
                "target.setNickname(source.getNickname());");
    }

    @Test
    void generateDtoFalseSkipsOnlyDto() {
        Compilation compilation = compile(entitySource(
                "test.emg.switches.nodto.entity.NoDtoEntity",
                "@AutoModel(value = \"NoDto\", generateDto = false)",
                field("Long", "id")
        ));

        assertGenerationSet(compilation, "test.emg.switches.nodto", "NoDto", false, true, true, true);
        assertNotContains(generatedSource(compilation, "test.emg.switches.nodto.converter.NoDtoConverter"), "toDto(", "toDtoList(");
    }

    @Test
    void generateBaseResponseFalseSkipsOnlyBaseResponse() {
        Compilation compilation = compile(entitySource(
                "test.emg.switches.nobase.entity.NoBaseEntity",
                "@AutoModel(value = \"NoBase\", generateBaseResponse = false)",
                field("Long", "id")
        ));

        assertGenerationSet(compilation, "test.emg.switches.nobase", "NoBase", true, false, true, true);
        assertNotContains(generatedSource(compilation, "test.emg.switches.nobase.converter.NoBaseConverter"),
                "toBaseResponse(", "toBaseResponseList(");
    }

    @Test
    void generateListResponseFalseSkipsOnlyListResponse() {
        Compilation compilation = compile(entitySource(
                "test.emg.switches.nolist.entity.NoListEntity",
                "@AutoModel(value = \"NoList\", generateListResponse = false)",
                field("Long", "id")
        ));

        assertGenerationSet(compilation, "test.emg.switches.nolist", "NoList", true, true, false, true);
        assertNotContains(generatedSource(compilation, "test.emg.switches.nolist.converter.NoListConverter"),
                "toListResponse(", "toListResponseList(");
    }

    @Test
    void generateConverterFalseSkipsOnlyConverter() {
        Compilation compilation = compile(entitySource(
                "test.emg.switches.noconverter.entity.NoConverterEntity",
                "@AutoModel(value = \"NoConverter\", generateConverter = false)",
                field("Long", "id")
        ));

        assertGenerationSet(compilation, "test.emg.switches.noconverter", "NoConverter", true, true, true, false);
    }

    @Test
    void processesMultipleEntitiesWithoutCrossContamination() {
        JavaFileObject customer = entitySource(
                "test.emg.multi.customer.entity.CustomerEntity",
                "@AutoModel(\"Customer\")",
                field("Long", "customerId"),
                field("String", "customerName")
        );
        JavaFileObject order = entitySource(
                "test.emg.multi.order.entity.OrderEntity",
                "@AutoModel(\"Order\")",
                field("Long", "orderId"),
                field("java.math.BigDecimal", "orderTotal")
        );

        Compilation compilation = compile(customer, order);

        assertGenerationSet(compilation, "test.emg.multi.customer", "Customer", true, true, true, true);
        assertGenerationSet(compilation, "test.emg.multi.order", "Order", true, true, true, true);
        String customerDto = generatedSource(compilation, "test.emg.multi.customer.model.dto.CustomerDTO");
        String orderDto = generatedSource(compilation, "test.emg.multi.order.model.dto.OrderDTO");
        assertContains(customerDto, "customerId", "customerName");
        assertNotContains(customerDto, "orderId", "orderTotal");
        assertContains(orderDto, "orderId", "orderTotal");
        assertNotContains(orderDto, "customerId", "customerName");
    }

    @Test
    void rejectsInvalidModelPrefixWithErrorDiagnostic() {
        Compilation compilation = compile(entitySource(
                "test.emg.invalid.prefix.entity.InvalidPrefixEntity",
                "@AutoModel(\"invalidPrefix\")",
                field("Long", "id")
        ));

        assertFailedWithError(compilation, "@AutoModel.value() 必须符合 Java 类名前缀规范");
    }

    @Test
    void rejectsResponseAliasConflictAndWritesNoPartialSources(@TempDir Path tempDir) {
        JavaFileObject entity = entitySource(
                "test.emg.invalid.conflict.entity.ConflictEntity",
                "@AutoModel(\"Conflict\")",
                field("String", "name"),
                field("String", "nickname", "@ResponseAlias(\"name\")")
        );

        Compilation compilation = compile(entity);
        assertFailedWithError(compilation, "Response 字段名冲突: name");

        DiskCompilation diskCompilation = compileOnDisk(tempDir, entity);
        assertFalse(diskCompilation.successful(), String.join(System.lineSeparator(), diskCompilation.diagnostics()));
        assertTrue(diskCompilation.diagnostics().stream()
                .anyMatch(message -> message.contains("Response 字段名冲突: name")));
        assertFalse(containsJavaSource(diskCompilation.generatedSources()),
                "Validation failure must not leave partially generated source files");
    }

    @Test
    void sameSourceCompilesTwiceIndependentlyWithoutFilerException() {
        Compilation first = compile(entitySource(
                "test.emg.repeat.entity.RepeatEntity",
                "@AutoModel(\"Repeat\")",
                field("Long", "id"),
                field("String", "name")
        ));
        Compilation second = compile(entitySource(
                "test.emg.repeat.entity.RepeatEntity",
                "@AutoModel(\"Repeat\")",
                field("Long", "id"),
                field("String", "name")
        ));

        assertGenerationSet(first, "test.emg.repeat", "Repeat", true, true, true, true);
        assertGenerationSet(second, "test.emg.repeat", "Repeat", true, true, true, true);
        assertNotContains(diagnostics(first), "FilerException");
        assertNotContains(diagnostics(second), "FilerException");
    }

    @Test
    void inputAndGeneratedSourcesExcludeLegacyFqn() {
        JavaFileObject entity = entitySource(
                "test.emg.identity.entity.IdentityEntity",
                "@AutoModel(\"Identity\")",
                field("Long", "id"),
                field("String", "name")
        );

        Compilation compilation = compile(entity);

        assertSucceeded(compilation);
        assertNotContains(charContent(entity), legacyFqn());
        for (JavaFileObject generatedSource : compilation.generatedSourceFiles()) {
            assertNotContains(charContent(generatedSource), legacyFqn());
        }
    }

    private static Compilation compile(JavaFileObject... sources) {
        return Compiler.javac()
                .withOptions("--release", "17")
                .withProcessors(new AutoModelProcessor())
                .compile(sources);
    }

    private static DiskCompilation compileOnDisk(Path tempDir, JavaFileObject source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new AssertionError("A full JDK with javac is required");
        }

        Path sourceDirectory = tempDir.resolve("src");
        Path classOutput = tempDir.resolve("classes");
        Path generatedSources = tempDir.resolve("generated");
        Path sourceFile = sourceDirectory.resolve("ConflictEntity.java");

        try {
            Files.createDirectories(sourceDirectory);
            Files.createDirectories(classOutput);
            Files.createDirectories(generatedSources);
            Files.writeString(sourceFile, source.getCharContent(false), StandardCharsets.UTF_8);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                    diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
                Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(List.of(sourceFile));
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

    private static boolean containsJavaSource(Path directory) {
        try (var files = Files.walk(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().endsWith(".java"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static JavaFileObject entitySource(String qualifiedName,
                                               String autoModelAnnotation,
                                               TestField... fields) {
        int separator = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, separator);
        String className = qualifiedName.substring(separator + 1);
        List<String> lines = new ArrayList<>();
        lines.add("package " + packageName + ";");
        lines.add("");
        lines.add("import io.github.forgottenlab.emg.annotations.*;");
        lines.add("");
        lines.add(autoModelAnnotation);
        lines.add("public class " + className + " {");

        for (TestField field : fields) {
            for (String annotation : field.annotations()) {
                lines.add("    " + annotation);
            }
            lines.add("    private " + field.type() + " " + field.name() + ";");
            lines.add("");
        }

        for (TestField field : fields) {
            String suffix = Character.toUpperCase(field.name().charAt(0)) + field.name().substring(1);
            lines.add("    public " + field.type() + " get" + suffix + "() {");
            lines.add("        return " + field.name() + ";");
            lines.add("    }");
            lines.add("");
            lines.add("    public void set" + suffix + "(" + field.type() + " " + field.name() + ") {");
            lines.add("        this." + field.name() + " = " + field.name() + ";");
            lines.add("    }");
            lines.add("");
        }

        lines.add("}");
        return JavaFileObjects.forSourceLines(qualifiedName, lines);
    }

    private static TestField field(String type, String name, String... annotations) {
        return new TestField(type, name, Arrays.asList(annotations));
    }

    private static void assertGenerationSet(Compilation compilation,
                                            String rootPackage,
                                            String prefix,
                                            boolean dto,
                                            boolean baseResponse,
                                            boolean listResponse,
                                            boolean converter) {
        assertSucceeded(compilation);
        assertSourceAndClassPresence(compilation, rootPackage + ".model.dto." + prefix + "DTO", dto);
        assertSourceAndClassPresence(compilation,
                rootPackage + ".model.response." + prefix + "BaseResponse", baseResponse);
        assertSourceAndClassPresence(compilation,
                rootPackage + ".model.response." + prefix + "ListResponse", listResponse);
        assertSourceAndClassPresence(compilation, rootPackage + ".converter." + prefix + "Converter", converter);
    }

    private static void assertGeneratedAndCompiled(Compilation compilation, String qualifiedName) {
        assertSourceAndClassPresence(compilation, qualifiedName, true);
    }

    private static void assertSourceAndClassPresence(Compilation compilation,
                                                     String qualifiedName,
                                                     boolean expected) {
        int separator = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, separator);
        String classFileName = qualifiedName.substring(separator + 1) + ".class";

        assertEquals(expected, compilation.generatedSourceFile(qualifiedName).isPresent(),
                "generated source presence for " + qualifiedName);
        assertEquals(expected,
                compilation.generatedFile(StandardLocation.CLASS_OUTPUT, packageName, classFileName).isPresent(),
                "compiled class presence for " + qualifiedName);
    }

    private static String generatedSource(Compilation compilation, String qualifiedName) {
        JavaFileObject source = compilation.generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("Missing generated source: " + qualifiedName));
        return charContent(source);
    }

    private static String charContent(JavaFileObject source) {
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

    private static String legacyFqn() {
        return String.join(".", "io", "github", "heran", "emg");
    }

    private record TestField(String type, String name, List<String> annotations) {
    }

    private record DiskCompilation(boolean successful, List<String> diagnostics, Path generatedSources) {
    }
}
