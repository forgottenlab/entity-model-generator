# Entity Model Generator (EMG)

**English** | [简体中文](README.zh-CN.md)

A Java APT compile-time tool that generates standard models and grouped Views from entities, reducing predictable boilerplate.

## ✨ Overview

Entity Model Generator (EMG) reads entity classes and annotations during `javac` compilation and writes real Java source files. Generated types are visible to IDEs, compilers, and static analysis tools. They require no runtime reflection and add no runtime overhead.

EMG currently provides two generation paths: V1 creates common DTO, Response, and Converter types; V2 creates custom single-table Views from field groups. The two paths can be used independently or triggered together by the same entity.

EMG is a focused entity-derived model generator with explicit rules and boundaries. It is not a general code generation platform that tries to infer every business concept. Requests, complex join results, and business-specific extension models should still be written by hand.

## 💡 Why EMG

A `UserEntity` often leads to `UserDTO`, `UserBaseResponse`, `UserListResponse`, and a field-copying Converter. These types are highly repetitive, yet field additions, exclusions, and renames are easy to miss across them.

EMG moves the predictable work to compile time:

- The entity remains the source of truth for fields.
- Annotations describe field rules for DTOs, Responses, and Views.
- The Processor writes visible, debuggable, statically checked Java source.
- Requests, aggregate models, and business-specific Responses remain handwritten.

This reduces boilerplate and lets code review focus on meaningful business differences. Convention over configuration. Common View generation requires only a group name after Maven setup.

## 🚀 Current Features

| Area | Current capability |
| --- | --- |
| V1 standard models | `@AutoModel` generates DTO, BaseResponse, ListResponse, and Converter types |
| V1 field rules | `@DtoIgnore`, `@ResponseIgnore`, `@ListIgnore`, and `@ResponseAlias` |
| V1 hardening | Primitives, arrays, generics, wildcards, nested types, simple-name conflicts, and boolean getter rules |
| V2 custom Views | `@AutoView`, repeatable container `@AutoViews`, and field groups with `@ViewGroups` |
| V2 composition | `@AutoView` works alone or together with `@AutoModel` |
| V2 defaults | `BaseName + CapitalizedGroup + View` in the `model.view` package |
| Compile safety | Full validation before source writes; failed rounds leave no partial generated output |
| Processor discovery | Handwritten `META-INF/services/javax.annotation.processing.Processor` SPI |

Generated models contain regular fields, getters, and setters. V1 Converter generation includes single-object and list conversion methods. V2 does not currently generate a Converter.

## 🧱 Modules

| Module | Responsibility |
| --- | --- |
| [`emg-annotations`](emg-annotations/) | Eight lightweight public annotations with no extra dependencies |
| [`emg-core`](emg-core/) | Metadata, constants, naming, and type utilities |
| [`emg-processor`](emg-processor/) | Resolvers, Validators, Generators, the Processor, and the handwritten SPI |
| [`emg-demo`](emg-demo/) | V1/V2 usage and compilation examples in the same Maven reactor |

The project uses a multi-module Maven build. `emg-processor` sets `proc=none` while compiling itself to prevent self-processing. Consumers load the Processor explicitly through `annotationProcessorPaths`.

## ✅ Requirements

- Java 17
- Maven

The verified environment is Windows 11, Oracle JDK 17.0.12, and Apache Maven 3.9.11. Validation also covered an independent Maven consumer and an initially empty, isolated Maven local repository.

Java 17 and Maven are the only formally verified toolchain scope. Gradle has not been verified, and formal JDK 21 compatibility is not claimed.

## 📦 Quick Start

### 1. Install EMG locally

Version `1.2.0` is not published to Maven Central. Before first use, clone the official repository and install the current source into your local Maven repository:

```bash
git clone https://github.com/forgottenlab/entity-model-generator.git
cd entity-model-generator
mvn clean install
```

### 2. Configure the consumer POM

The following is a copyable minimum Maven configuration. `emg-annotations` is a regular compile dependency, while `emg-processor` appears only in `annotationProcessorPaths`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>emg-consumer</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.github.forgottenlab.emg</groupId>
            <artifactId>emg-annotations</artifactId>
            <version>1.2.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <release>17</release>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>io.github.forgottenlab.emg</groupId>
                            <artifactId>emg-processor</artifactId>
                            <version>1.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. Declare a minimal View

```java
package com.example.product.entity;

import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.annotations.ViewGroups;

@AutoView("basic")
public class ProductEntity {

    @ViewGroups("basic")
    private Long id;

    @ViewGroups("basic")
    private String name;
}
```

### 4. Compile the consumer project

```bash
mvn clean compile
```

The default generated-source directory is:

```text
target/generated-sources/annotations
```

This example generates `com.example.product.model.view.ProductBasicView`, whose simple class name is `ProductBasicView`.

Maven adds generated source to the same compilation automatically. No Spring, MyBatis, Lombok, runtime reflection, extra YAML, manual `-s`, or manual generated-sources registration is required.

## ⚙️ Maven Configuration

The consumer Maven configuration has two distinct responsibilities:

1. Place `io.github.forgottenlab.emg:emg-annotations:1.2.0` in regular `dependencies` so source code can reference the public annotations.
2. Place `io.github.forgottenlab.emg:emg-processor:1.2.0` in the `maven-compiler-plugin` `annotationProcessorPaths` so `javac` can discover and run the Processor.

Consumers do not need `emg-core` as a regular dependency; the Processor obtains it through its own Maven dependencies. The service descriptor is `META-INF/services/javax.annotation.processing.Processor`, which registers `io.github.forgottenlab.emg.processor.AutoModelProcessor`.

A standard Maven build generates, registers, and compiles the source under `target/generated-sources/annotations`. The IDE is an editor aid, not the source of truth for generation success.

## 🧩 V1 Standard Model Generation

The shortest V1 source needs only `@AutoModel`. When Converter generation is enabled, participating entity fields must expose matching public, instance, zero-argument getters:

```java
package com.example.user.entity;

import io.github.forgottenlab.emg.annotations.AutoModel;

@AutoModel("User")
public class UserEntity {

    private Long id;

    public Long getId() {
        return id;
    }
}
```

Four types are generated by default:

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

The `@AutoModel` options `generateDto`, `generateBaseResponse`, `generateListResponse`, and `generateConverter` can disable individual outputs.

Field annotations have the following V1 responsibilities:

| Annotation | V1 behavior |
| --- | --- |
| `@DtoIgnore` | Excludes the field from the DTO |
| `@ResponseIgnore` | Excludes the field from both BaseResponse and ListResponse |
| `@ListIgnore` | Excludes the field from ListResponse only; BaseResponse retains it |
| `@ResponseAlias("name")` | Uses the alias in both Responses and their Converter mappings |

```java
@DtoIgnore
@ResponseIgnore
private String password;

@ResponseAlias("name")
private String nickname;

@ListIgnore
private String internalNote;
```

Depending on the enabled targets, the Converter generates `toDto`, `toBaseResponse`, `toListResponse`, and corresponding list methods. A `null` object input returns `null`; a `null` list input also returns `null`; a `null` element inside a list becomes a `null` output element.

Regular fields and boxed `Boolean` use `getXxx()`. A primitive `boolean` prefers `isXxx()` and falls back to `getXxx()`. A getter must be public, non-static, have no parameters, and return exactly the field type. Otherwise, the Processor reports a compilation error before writing any generated source.

## 🧭 V2 Grouped Custom Views

V2 declares field groups with `@ViewGroups` and requests Views with one or more `@AutoView` annotations. The `ProductEntity` in Quick Start demonstrates the shortest standalone path and does not need `@AutoModel`.

`@AutoView` is a Java repeatable annotation backed by the `@AutoViews` container. In normal source code, declare `@AutoView` more than once instead of writing the container directly.

A field can belong to one or more groups, `@AutoView` is repeatable, and `name` can override the final simple class name:

```java
package com.example.user.entity;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.annotations.ViewGroups;

@AutoModel("User")
@AutoView("basic")
@AutoView(value = "detail", name = "UserProfileView")
public class UserEntity {

    @ViewGroups({"basic", "detail"})
    private Long id;

    @ViewGroups("basic")
    private String username;

    @ViewGroups("detail")
    private String phone;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }
}
```

This example generates the four V1 standard types plus `UserBasicView` and `UserProfileView`. With only `@AutoView`, EMG generates only Views. With only `@AutoModel`, V1 behavior is unchanged. When both are present, the two output sets share one source analysis.

View membership is controlled only by `@ViewGroups`. `@DtoIgnore`, `@ResponseIgnore`, `@ListIgnore`, and `@ResponseAlias` neither exclude nor rename View fields. `@ViewGroups` uses `RetentionPolicy.CLASS`, but reading that metadata from already compiled classes in another module has not yet been verified.

If `@AutoView("basic")` has no directly declared, non-`static` field in `basic`, compilation reports a missing-group diagnostic on the source class. All V1/V2 candidates complete full-round validation before any Filer write, so a failed round leaves no partial `.java` output.

A View currently contains only fields, getters, and setters. It does not include a Converter, constructor, builder, record, or Lombok code.

## 🏷️ Naming and Package Conventions

For a source type named `com.example.user.entity.UserEntity`:

| Output | Default class name | Default package |
| --- | --- | --- |
| DTO | `UserDTO` | `com.example.user.model.dto` |
| BaseResponse | `UserBaseResponse` | `com.example.user.model.response` |
| ListResponse | `UserListResponse` | `com.example.user.model.response` |
| Converter | `UserConverter` | `com.example.user.converter` |
| View for group `basic` | `UserBasicView` | `com.example.user.model.view` |

The V2 BaseName uses `@AutoModel.value()` when present. Otherwise, it uses the source simple name with a trailing `Entity` removed. The default View name is `BaseName + CapitalizedGroup + View`. `@AutoView.name()` is an optional complete simple-class-name override and cannot contain a package path.

When the source package ends in `.entity`, EMG replaces that suffix. Otherwise, it appends the target package segment to the source package. Group and name values must be legal Java identifiers, cannot be keywords, and are not trimmed. Field order and the order of multiple Views follow source declaration order.

## 🛡️ Compile-Time Validation

EMG follows this processing flow:

```text
Entity and annotations
  -> shared source metadata
  -> V1/V2 metadata
  -> full-round validation
  -> Java source generation
  -> compilation of generated sources
```

Before calling the Filer, the Processor validates non-class inputs, empty or illegal names, missing groups, duplicate target FQNs, conflicts with existing source types, unsupported types, and Converter getters. A failure in any candidate prevents all generation in that round, avoiding partial source output.

Type analysis is based on compile-time `TypeMirror` structures. It supports primitives, declared types, arrays, parameterized types, wildcards, and nested types. Simple-name conflicts fall back to fully qualified type names. Unsupported kinds such as `TYPEVAR`, `ERROR`, `INTERSECTION`, `UNION`, `NONE`, and `NULL` produce source-located diagnostics.

V1 and V2 read only directly declared, non-`static` fields from the source class; inherited fields are not collected. Do not edit generated source because the next compilation will overwrite it. Extend business behavior through inheritance, composition, or handwritten dedicated types.

## 🎯 Demo

The [`emg-demo`](emg-demo/) [`UserEntity`](emg-demo/src/main/java/io/github/forgottenlab/emg/demo/user/entity/UserEntity.java) combines `@AutoModel`, two `@AutoView` declarations, ignore and alias rules, and fields assigned to multiple groups.

A full reactor compilation generates and compiles:

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`
- `UserBasicView`
- `UserProfileView`

The Demo also shows generated types in service/controller-style code and extends the base Response with a handwritten `UserLoginResponse`. It does not depend on Spring or real MyBatis. Requests remain handwritten because endpoint parameters, validation, and business semantics cannot be reliably derived from an entity alone.

## 🧪 Testing

The current Processor baseline contains 62 tests:

| Test class | Count | Coverage |
| --- | ---: | --- |
| `AutoModelProcessorCompilationTest` | 14 | V1 generation, switches, ignores, aliases, multiple entities, and repeat compilation |
| `AutoModelProcessorHardeningTest` | 15 | Types, field boundaries, names, conflicts, and getters |
| `AutoModelProcessorSpiTest` | 3 | SPI, ServiceLoader, and supported annotation types |
| `AutoViewProcessorCompilationTest` | 20 | V2 success cases, composition, types, order, and retention |
| `AutoViewProcessorValidationTest` | 10 | V2 diagnostics, conflicts, and zero partial output |

The latest verified result is 62 tests, 0 failures, 0 errors, and 0 skipped. Tests exercise real `javac`/APT/SPI paths and were cross-checked with an independent consumer and an isolated Maven local repository.

Run from the repository root:

```bash
mvn clean test
```

This evidence applies only to the current Java 17 + Maven scope. It does not establish Gradle, JDK 21, or release readiness.

## 🛠️ Troubleshooting / FAQ

### No classes are generated

Confirm that both the regular annotations dependency and `annotationProcessorPaths` are present, verify that `mvn -version` uses JDK 17, and run `mvn clean compile`. The source must be a class annotated with `@AutoModel` or `@AutoView`; `@ViewGroups` alone does not trigger generation.

### A group is missing or invalid

`@AutoView("basic")` needs at least one directly declared, non-`static` field annotated with `@ViewGroups("basic")`. Group values are not trimmed, so `" basic"` and `"basic "` are invalid.

### The Converter reports a missing getter

When `generateConverter = true`, every field included in an enabled target needs a matching public, instance, zero-argument getter. Check visibility, parameters, the exact return type, and the naming difference between primitive `boolean` and boxed `Boolean`.

### IDEA cannot see a generated type yet

Use `mvn clean compile` as the source of truth, then inspect `target/generated-sources/annotations`. Some IDEA versions or import modes may require marking that directory as a Generated Sources Root; this does not affect Maven compilation.

### Why does `@ResponseAlias` not affect a View?

It controls only V1 Response names and Converter mappings. View members are independently selected by `@ViewGroups` and retain the original entity field names.

### Can EMG be downloaded directly from Maven Central?

Not currently. Run `mvn clean install` in the EMG repository first, then let the consumer resolve the `1.2.0` artifacts from the local Maven repository.

## ⚠️ Current Limitations

- Version `1.2.0` is not published to Maven Central; EMG must be installed locally with `mvn clean install` before use.
- No stable Release has been published, and the project does not claim production readiness.
- Only Java 17 and Maven are formally verified. Gradle is unverified, and formal JDK 21 compatibility is not claimed.
- JoinView, View Converter, and SQL parsing are not supported.
- Custom output packages are not configurable; Views use the fixed `model.view` convention.
- Only directly declared, non-`static` source fields are collected; inherited fields are excluded.
- Reading compiled `@ViewGroups` CLASS-retention metadata across modules has not been verified.
- Requests are not generated. Complex joins, aggregates, and complex projections remain handwritten.
- Views do not generate constructors, builders, records, or Lombok code.

## 🗺️ Roadmap

V1 standard model generation and V2 grouped single-table Views are implemented. The next step is to prepare the bilingual documentation PR and audit version strategy, CHANGELOG, LICENSE, and pre-release requirements.

The roadmap is not a release commitment. JoinView, SQL, custom packages, Gradle support, JDK 21 support, and Maven Central publication are neither implemented nor promised for the current version. Any later feature requires separate design, validation, and authorization.

## 🤝 Contributing and Feedback

Use [GitHub Issues](https://github.com/forgottenlab/entity-model-generator/issues) for reproducible bug reports or focused proposals. Keep changes small, preserve the existing V1/V2 APIs, and update relevant tests plus both README languages together.

The project grew from the practical cost of repeatedly maintaining DTOs, Responses, and Converters. Thanks to everyone who has helped through learning, use, and feedback.

## 📜 License

This repository currently has no `LICENSE` file, so this documentation does not claim that any open-source license applies. Do not infer permission to copy, modify, or distribute the project from the README until an explicit license is added and reviewed.
