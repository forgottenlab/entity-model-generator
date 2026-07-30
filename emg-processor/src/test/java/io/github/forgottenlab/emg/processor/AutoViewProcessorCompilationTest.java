package io.github.forgottenlab.emg.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.forgottenlab.emg.annotations.ViewGroups;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoViewProcessorCompilationTest {

    @Test
    void standardOnlyStillGeneratesV1Models() {
        Compilation compilation = compile(source("test.emg.v2.standard.entity.UserEntity", """
                package test.emg.v2.standard.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;

                @AutoModel("User")
                public class UserEntity {
                    private Long id;
                    public Long getId() { return id; }
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.standard.model.dto.UserDTO");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.standard.model.response.UserBaseResponse");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.standard.model.response.UserListResponse");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.standard.converter.UserConverter");
        assertNotGenerated(compilation, "test.emg.v2.standard.model.view.UserBasicView");
    }

    @Test
    void customViewOnlyDoesNotRequireAutoModel() {
        Compilation compilation = compile(source("test.emg.v2.only.entity.CustomEntity", """
                package test.emg.v2.only.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class CustomEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.only.model.view.CustomBasicView");
        assertNotGenerated(compilation, "test.emg.v2.only.model.dto.CustomDTO");
        assertNotGenerated(compilation, "test.emg.v2.only.converter.CustomConverter");
    }

    @Test
    void autoModelAndAutoViewCanBeCombined() {
        Compilation compilation = compile(source("test.emg.v2.combined.entity.AccountEntity", """
                package test.emg.v2.combined.entity;

                import io.github.forgottenlab.emg.annotations.AutoModel;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoModel("Member")
                @AutoView("basic")
                public class AccountEntity {
                    @ViewGroups("basic")
                    private Long id;
                    public Long getId() { return id; }
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.combined.model.dto.MemberDTO");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.combined.converter.MemberConverter");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.combined.model.view.MemberBasicView");
    }

    @Test
    void singleGroupGeneratesDefaultNamedView() {
        Compilation compilation = compile(source("test.emg.v2.naming.entity.UserEntity", """
                package test.emg.v2.naming.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.naming.model.view.UserBasicView");
        assertContains(view,
                "package test.emg.v2.naming.model.view;",
                "public class UserBasicView",
                "private Long id;",
                "public Long getId()",
                "public void setId(Long id)");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.naming.model.view.UserBasicView");
    }

    @Test
    void customViewNameOverridesConvention() {
        Compilation compilation = compile(source("test.emg.v2.customname.entity.UserEntity", """
                package test.emg.v2.customname.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView(value = "basic", name = "UserSimpleInfo")
                public class UserEntity {
                    @ViewGroups("basic")
                    private String username;
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.customname.model.view.UserSimpleInfo");
        assertNotGenerated(compilation, "test.emg.v2.customname.model.view.UserBasicView");
    }

    @Test
    void repeatableAutoViewsGenerateMultipleViews() {
        Compilation compilation = compile(source("test.emg.v2.repeatable.entity.UserEntity", """
                package test.emg.v2.repeatable.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                @AutoView("detail")
                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                    @ViewGroups("detail")
                    private String phone;
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.repeatable.model.view.UserBasicView");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.repeatable.model.view.UserDetailView");
    }

    @Test
    void fieldCanBelongToMultipleGroups() {
        Compilation compilation = compile(source("test.emg.v2.multigroup.entity.UserEntity", """
                package test.emg.v2.multigroup.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                @AutoView("detail")
                public class UserEntity {
                    @ViewGroups({"basic", "detail"})
                    private Long id;
                    @ViewGroups("basic")
                    private String username;
                    @ViewGroups("detail")
                    private String phone;
                }
                """));

        assertSucceeded(compilation);
        String basic = generatedSource(compilation, "test.emg.v2.multigroup.model.view.UserBasicView");
        String detail = generatedSource(compilation, "test.emg.v2.multigroup.model.view.UserDetailView");
        assertContains(basic, "private Long id;", "private String username;");
        assertNotContains(basic, "phone");
        assertContains(detail, "private Long id;", "private String phone;");
        assertNotContains(detail, "username");
        assertTrue(basic.indexOf("private Long id;") < basic.indexOf("private String username;"));
        assertTrue(detail.indexOf("private Long id;") < detail.indexOf("private String phone;"));
    }

    @Test
    void duplicateFieldGroupsAreDeduplicated() {
        Compilation compilation = compile(source("test.emg.v2.duplicatefield.entity.UserEntity", """
                package test.emg.v2.duplicatefield.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups({"basic", "basic"})
                    private Long id;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.duplicatefield.model.view.UserBasicView");
        assertEquals(1, occurrences(view, "private Long id;"));
        assertEquals(1, occurrences(view, "public Long getId()"));
    }

    @Test
    void ungroupedFieldIsExcluded() {
        Compilation compilation = compile(source("test.emg.v2.ungrouped.entity.UserEntity", """
                package test.emg.v2.ungrouped.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                    private String password;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.ungrouped.model.view.UserBasicView");
        assertContains(view, "private Long id;");
        assertNotContains(view, "password", "Password");
    }

    @Test
    void responseAliasDoesNotRenameViewField() {
        Compilation compilation = compile(source("test.emg.v2.alias.entity.UserEntity", """
                package test.emg.v2.alias.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ResponseAlias;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ResponseAlias("displayName")
                    @ViewGroups("basic")
                    private String nickname;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.alias.model.view.UserBasicView");
        assertContains(view, "private String nickname;", "getNickname()", "setNickname(String nickname)");
        assertNotContains(view, "displayName", "DisplayName");
    }

    @Test
    void v1IgnoreAnnotationsDoNotExcludeViewFields() {
        Compilation compilation = compile(source("test.emg.v2.ignoreboundary.entity.UserEntity", """
                package test.emg.v2.ignoreboundary.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.DtoIgnore;
                import io.github.forgottenlab.emg.annotations.ListIgnore;
                import io.github.forgottenlab.emg.annotations.ResponseIgnore;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @DtoIgnore @ResponseIgnore @ListIgnore @ViewGroups("basic")
                    private String internalValue;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.ignoreboundary.model.view.UserBasicView");
        assertContains(view, "private String internalValue;", "getInternalValue()", "setInternalValue(");
    }

    @Test
    void viewGroupsAloneDoesNotGenerateView() {
        Compilation compilation = compile(source("test.emg.v2.noautoview.entity.UserEntity", """
                package test.emg.v2.noautoview.entity;

                import io.github.forgottenlab.emg.annotations.ViewGroups;

                public class UserEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """));

        assertSucceeded(compilation);
        assertTrue(compilation.generatedSourceFiles().isEmpty());
    }

    @Test
    void staticFieldIsExcludedFromView() {
        Compilation compilation = compile(source("test.emg.v2.staticfield.entity.UserEntity", """
                package test.emg.v2.staticfield.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class UserEntity {
                    @ViewGroups("basic")
                    private static String globalName;
                    @ViewGroups("basic")
                    private Long id;
                }
                """));

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.staticfield.model.view.UserBasicView");
        assertContains(view, "private Long id;");
        assertNotContains(view, "globalName", "GlobalName");
    }

    @Test
    void inheritedFieldIsExcludedFromView() {
        JavaFileObject parent = source("test.emg.v2.inheritance.entity.ParentEntity", """
                package test.emg.v2.inheritance.entity;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                public class ParentEntity {
                    @ViewGroups("basic") protected String inheritedValue;
                }
                """);
        JavaFileObject child = source("test.emg.v2.inheritance.entity.ChildEntity", """
                package test.emg.v2.inheritance.entity;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                @AutoView("basic")
                public class ChildEntity extends ParentEntity {
                    @ViewGroups("basic") private String directValue;
                }
                """);

        Compilation compilation = compile(parent, child);

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.inheritance.model.view.ChildBasicView");
        assertContains(view, "private String directValue;");
        assertNotContains(view, "inheritedValue", "InheritedValue");
    }

    @Test
    void sourceOutsideEntityPackageAppendsViewSegment() {
        Compilation compilation = compile(source("test.emg.v2.audit.AuditRecord", """
                package test.emg.v2.audit;
                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                @AutoView("summary")
                public class AuditRecord {
                    @ViewGroups("summary") private Long id;
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.audit.model.view.AuditRecordSummaryView");
    }

    @Test
    void complexFieldTypesCompileInView() {
        JavaFileObject nested = source("test.emg.v2.types.ExternalTypes", """
                package test.emg.v2.types;
                public final class ExternalTypes {
                    public static final class Nested { }
                }
                """);
        JavaFileObject entity = source("test.emg.v2.types.entity.TypeEntity", """
                package test.emg.v2.types.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;
                import test.emg.v2.types.ExternalTypes;

                @AutoView("detail")
                public class TypeEntity {
                    @ViewGroups("detail") private int count;
                    @ViewGroups("detail") private String[] labels;
                    @ViewGroups("detail") private java.util.List<? extends Number> values;
                    @ViewGroups("detail") private java.util.Map<String, java.util.List<Long>> groupedIds;
                    @ViewGroups("detail") private ExternalTypes.Nested nested;
                }
                """);

        Compilation compilation = compile(nested, entity);

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.types.model.view.TypeDetailView");
        assertContains(view,
                "private int count;",
                "private String[] labels;",
                "private List<? extends Number> values;",
                "private Map<String, List<Long>> groupedIds;",
                "private ExternalTypes.Nested nested;",
                "import java.util.List;",
                "import java.util.Map;",
                "import test.emg.v2.types.ExternalTypes;");
        assertGeneratedAndCompiled(compilation, "test.emg.v2.types.model.view.TypeDetailView");
    }

    @Test
    void conflictingSimpleTypesUseQualifiedNames() {
        JavaFileObject alpha = source("test.emg.v2.collision.alpha.Shared", """
                package test.emg.v2.collision.alpha;
                public final class Shared { }
                """);
        JavaFileObject beta = source("test.emg.v2.collision.beta.Shared", """
                package test.emg.v2.collision.beta;
                public final class Shared { }
                """);
        JavaFileObject entity = source("test.emg.v2.collision.entity.CollisionEntity", """
                package test.emg.v2.collision.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("detail")
                public class CollisionEntity {
                    @ViewGroups("detail") private test.emg.v2.collision.alpha.Shared first;
                    @ViewGroups("detail") private test.emg.v2.collision.beta.Shared second;
                }
                """);

        Compilation compilation = compile(alpha, beta, entity);

        assertSucceeded(compilation);
        String view = generatedSource(compilation, "test.emg.v2.collision.model.view.CollisionDetailView");
        assertContains(view,
                "private test.emg.v2.collision.alpha.Shared first;",
                "private test.emg.v2.collision.beta.Shared second;");
        assertNotContains(view,
                "import test.emg.v2.collision.alpha.Shared;",
                "import test.emg.v2.collision.beta.Shared;");
    }

    @Test
    void sameCustomViewSourceCompilesTwiceIndependently() {
        Compilation first = compile(repeatSource());
        Compilation second = compile(repeatSource());

        assertSucceeded(first);
        assertSucceeded(second);
        assertGeneratedAndCompiled(first, "test.emg.v2.independent.model.view.RepeatBasicView");
        assertGeneratedAndCompiled(second, "test.emg.v2.independent.model.view.RepeatBasicView");
        assertEquals(
                generatedSource(first, "test.emg.v2.independent.model.view.RepeatBasicView"),
                generatedSource(second, "test.emg.v2.independent.model.view.RepeatBasicView")
        );
        assertFalse(diagnostics(first).contains("FilerException"));
        assertFalse(diagnostics(second).contains("FilerException"));
    }

    @Test
    void viewGroupsUsesClassRetention() {
        Retention retention = ViewGroups.class.getAnnotation(Retention.class);
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    void unicodeGroupProducesValidDefaultName() {
        Compilation compilation = compile(source("test.emg.v2.unicode.entity.UserEntity", """
                package test.emg.v2.unicode.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("用户")
                public class UserEntity {
                    @ViewGroups("用户")
                    private String name;
                }
                """));

        assertSucceeded(compilation);
        assertGeneratedAndCompiled(compilation, "test.emg.v2.unicode.model.view.User用户View");
    }

    private static JavaFileObject repeatSource() {
        return source("test.emg.v2.independent.entity.RepeatEntity", """
                package test.emg.v2.independent.entity;

                import io.github.forgottenlab.emg.annotations.AutoView;
                import io.github.forgottenlab.emg.annotations.ViewGroups;

                @AutoView("basic")
                public class RepeatEntity {
                    @ViewGroups("basic")
                    private Long id;
                }
                """);
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

    private static String generatedSource(Compilation compilation, String qualifiedName) {
        JavaFileObject source = compilation.generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("Missing generated source: " + qualifiedName));
        try {
            return source.getCharContent(false).toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void assertGeneratedAndCompiled(Compilation compilation, String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, separator);
        String classFileName = qualifiedName.substring(separator + 1) + ".class";
        assertTrue(compilation.generatedSourceFile(qualifiedName).isPresent(), qualifiedName);
        assertTrue(compilation.generatedFile(
                StandardLocation.CLASS_OUTPUT, packageName, classFileName
        ).isPresent(), qualifiedName + " class output");
    }

    private static void assertNotGenerated(Compilation compilation, String qualifiedName) {
        assertTrue(compilation.generatedSourceFile(qualifiedName).isEmpty(), qualifiedName);
    }

    private static void assertSucceeded(Compilation compilation) {
        assertEquals(Compilation.Status.SUCCESS, compilation.status(), diagnostics(compilation));
    }

    private static String diagnostics(Compilation compilation) {
        StringBuilder result = new StringBuilder();
        compilation.diagnostics().forEach(diagnostic -> result
                .append(diagnostic.getKind())
                .append(": ")
                .append(diagnostic.getMessage(null))
                .append(System.lineSeparator()));
        return result.toString();
    }

    private static int occurrences(String source, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }

    private static void assertContains(String source, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), () -> "Expected source to contain: " + fragment);
        }
    }

    private static void assertNotContains(String source, String... fragments) {
        for (String fragment : fragments) {
            assertFalse(source.contains(fragment), () -> "Expected source not to contain: " + fragment);
        }
    }
}
