# Entity Model Generator (EMG)

[English](README.md) | **简体中文**

基于 Java APT 的编译期工具，从 Entity 生成标准模型和字段分组 View，减少可预测的重复代码。

## ✨ 项目概览

Entity Model Generator（EMG）在 `javac` 编译阶段读取实体类和注解，并生成真实的 Java 源文件。生成结果可被 IDE、编译器和静态分析工具直接识别，不依赖运行时反射，也不会增加运行期开销。

EMG 当前包含两条生成路径：V1 负责常用的 DTO、Response 和 Converter；V2 根据字段分组生成自定义单表 View。两条路径可以独立使用，也可以由同一个 Entity 同时触发。

EMG 的定位是边界清晰、规则明确的实体派生模型生成器，而不是试图推断全部业务语义的通用代码生成平台。Request、复杂联表结果和业务专属扩展模型仍应由开发者手写。

## 💡 为什么需要 EMG

一个 `UserEntity` 经常会继续派生出 `UserDTO`、`UserBaseResponse`、`UserListResponse` 和字段拷贝 Converter。这些类型高度相似，却容易在字段新增、裁剪或重命名时漏改。

EMG 将可重复的部分交给编译期生成：

- Entity 继续作为字段事实来源。
- 注解描述 DTO、Response 和 View 的字段规则。
- Processor 生成可见、可调试、可静态检查的 Java 源码。
- 业务语义强的 Request、聚合模型和扩展 Response 保持手写。

这样可以减少样板代码，并让代码审查更聚焦于业务差异。约定优于配置。完成 Maven 配置后，常见 View 生成只需要一个 group 名称。

## 🚀 当前功能

| 范围 | 当前能力 |
| --- | --- |
| V1 标准模型 | `@AutoModel` 生成 DTO、BaseResponse、ListResponse 和 Converter |
| V1 字段规则 | `@DtoIgnore`、`@ResponseIgnore`、`@ListIgnore`、`@ResponseAlias` |
| V1 加固 | primitive、数组、泛型、通配符、嵌套类型、同简单名冲突及 boolean getter 规则 |
| V2 自定义 View | `@AutoView`、可重复容器 `@AutoViews`、字段分组 `@ViewGroups` |
| V2 组合方式 | `@AutoView` 可独立使用，也可与 `@AutoModel` 同时使用 |
| V2 默认规则 | `BaseName + CapitalizedGroup + View`，默认包为 `model.view` |
| 编译安全 | 在写入生成源码前统一校验；失败时不留下部分生成输出 |
| Processor 发现 | 手写 `META-INF/services/javax.annotation.processing.Processor` SPI |
| Processor 兼容性 | 独立 fixture 以两种 processor path 顺序编译 EMG、Lombok 1.18.42 与 MapStruct 1.6.3 |

生成模型包含普通字段、getter 和 setter。V1 Converter 生成单对象与列表转换方法；V2 当前不生成 Converter。

## 🧱 模块结构

| 模块 | 职责 |
| --- | --- |
| [`emg-annotations`](emg-annotations/) | 八个公开注解，保持轻量且无额外依赖 |
| [`emg-core`](emg-core/) | 元数据、常量、命名和类型工具 |
| [`emg-processor`](emg-processor/) | Resolver、Validator、Generator、Processor 和手写 SPI |
| [`emg-demo`](emg-demo/) | 同一 Maven reactor 中的 V1/V2 使用与编译示例 |

项目使用 Maven 多模块构建。`emg-processor` 编译自身时设置 `proc=none`，避免 Processor 自处理；消费者通过 `annotationProcessorPaths` 显式加载 Processor。

`integration-fixtures/plain-emg` 与 `integration-fixtures/annotation-processors` 是独立消费者，不属于 reactor 模块。它们只通过 Maven 坐标解析 EMG，分别验证普通 EMG 接入以及 EMG/Lombok/MapStruct 协作。

## ✅ 环境要求

- Java 17
- Maven

当前已验证环境为 Windows 11、Oracle JDK 17.0.12 和 Apache Maven 3.9.11。验证还覆盖了独立 Maven 消费者和空的隔离 Maven local repository。

Java 17 和 Maven 是当前唯一正式验证的工具链范围。项目尚未验证 Gradle，也未承诺 JDK 21 正式兼容。

CI workflow 面向 `ubuntu-latest` 上的 Temurin Java 17。其命令已在本地复现，但 hosted GitHub Actions 结果需在 push 或 pull request 后实际运行才能确认；当前不宣称 passing，也不展示通过徽章。

## 📦 快速开始

### 1. 在本地安装 EMG

`1.2.0` 尚未发布到 Maven Central。首次使用前，请克隆正式仓库并将当前源码安装到本地 Maven repository：

```bash
git clone https://github.com/forgottenlab/entity-model-generator.git
cd entity-model-generator
mvn clean install
```

### 2. 配置消费者 POM

下面是可复制的最小 Maven 配置。`emg-annotations` 是普通编译依赖，`emg-processor` 只放在 `annotationProcessorPaths` 中：

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

### 3. 声明最小 View

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

### 4. 编译消费者项目

```bash
mvn clean compile
```

默认生成源码目录为：

```text
target/generated-sources/annotations
```

上例会生成 `com.example.product.model.view.ProductBasicView`，类简单名为 `ProductBasicView`。

Maven 会自动把生成源码加入同一次编译。不需要 Spring、MyBatis、Lombok、运行时反射、额外 YAML、手工 `-s` 或手工注册 generated-sources。

## ⚙️ Maven 配置

消费者 Maven 配置有两个不同职责：

1. `io.github.forgottenlab.emg:emg-annotations:1.2.0` 放在普通 `dependencies` 中，使源码可以引用公开注解。
2. `io.github.forgottenlab.emg:emg-processor:1.2.0` 放在 `maven-compiler-plugin` 的 `annotationProcessorPaths` 中，使 `javac` 可以发现并运行 Processor。

消费者不需要把 `emg-core` 声明为普通依赖；Processor 会通过自身 Maven 依赖获得它。Processor 的服务描述文件为 `META-INF/services/javax.annotation.processing.Processor`，其中注册 `io.github.forgottenlab.emg.processor.AutoModelProcessor`。

标准 Maven 构建会生成、注册并编译 `target/generated-sources/annotations` 中的源码。IDE 只负责编辑体验，不是生成成功的事实来源。

## 🧩 V1 标准模型生成

最短的 V1 来源只需要 `@AutoModel`。当启用 Converter 时，参与转换的实体字段必须提供匹配的 public 实例零参 getter：

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

默认生成四个类型：

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

`@AutoModel` 的 `generateDto`、`generateBaseResponse`、`generateListResponse` 和 `generateConverter` 可分别关闭对应输出。

字段注解职责如下：

| 注解 | V1 行为 |
| --- | --- |
| `@DtoIgnore` | 从 DTO 中排除字段 |
| `@ResponseIgnore` | 从 BaseResponse 和 ListResponse 中排除字段 |
| `@ListIgnore` | 只从 ListResponse 中排除字段，BaseResponse 保留 |
| `@ResponseAlias("name")` | 在两个 Response 及对应 Converter 映射中使用别名 |

```java
@DtoIgnore
@ResponseIgnore
private String password;

@ResponseAlias("name")
private String nickname;

@ListIgnore
private String internalNote;
```

根据启用的目标，Converter 生成 `toDto`、`toBaseResponse`、`toListResponse` 及对应的列表方法。单对象输入为 `null` 时返回 `null`；列表输入为 `null` 时也返回 `null`；列表中的 `null` 元素会转换为 `null` 元素。

普通字段和包装 `Boolean` 使用 `getXxx()`。primitive `boolean` 优先使用 `isXxx()`，不存在时再使用 `getXxx()`。Getter 必须为 public、实例、零参，且返回类型与字段完全匹配；否则 Processor 在生成任何源码前报告编译错误。

## 🧭 V2 字段分组与自定义 View

V2 通过 `@ViewGroups` 声明字段分组，通过一个或多个 `@AutoView` 请求生成 View。快速开始中的 `ProductEntity` 展示了可独立使用的最短路径，无需附加 `@AutoModel`。

`@AutoView` 是由 `@AutoViews` 容器支持的 Java 可重复注解。通常直接重复书写 `@AutoView`，无需手工使用容器。

字段可以属于一个或多个 group，`@AutoView` 可重复使用，`name` 可覆盖最终类简单名：

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

该示例同时生成 V1 四个标准类型，以及 `UserBasicView` 和 `UserProfileView`。只有 `@AutoView` 时只生成 View；只有 `@AutoModel` 时保持 V1 行为；两者同时存在时共享同一次来源解析并生成两组输出。

View 是否包含字段只由 `@ViewGroups` 决定。`@DtoIgnore`、`@ResponseIgnore`、`@ListIgnore` 和 `@ResponseAlias` 不排除或重命名 View 字段。`@ViewGroups` 使用 `RetentionPolicy.CLASS`，但当前尚未验证从其他模块已编译 class 中读取这些元数据。

如果 `@AutoView("basic")` 没有任何直接声明的非 `static` 字段属于 `basic`，编译会给出指向来源类的缺失 group 诊断。所有 V1/V2 候选都在写入 Filer 前完成整轮预检，因此失败时不会留下部分 `.java` 输出。

View 当前只生成字段、getter 和 setter，不生成 Converter、构造器、builder、record 或 Lombok 代码。

## 🏷️ 命名与包约定

假设来源类型为 `com.example.user.entity.UserEntity`：

| 输出 | 默认类名 | 默认包 |
| --- | --- | --- |
| DTO | `UserDTO` | `com.example.user.model.dto` |
| BaseResponse | `UserBaseResponse` | `com.example.user.model.response` |
| ListResponse | `UserListResponse` | `com.example.user.model.response` |
| Converter | `UserConverter` | `com.example.user.converter` |
| group 为 `basic` 的 View | `UserBasicView` | `com.example.user.model.view` |

V2 BaseName 优先使用 `@AutoModel.value()`；没有 `@AutoModel` 时使用来源类简单名，并移除末尾 `Entity`。默认 View 名为 `BaseName + CapitalizedGroup + View`。`@AutoView.name()` 是可选的完整类简单名覆盖，不能包含包路径。

如果来源包以 `.entity` 结尾，EMG 会替换该后缀；否则在来源包后追加目标包段。group 和 name 必须是合法 Java 标识符且不能是关键字，不会自动 trim。字段和多个 View 的输出顺序保持源码声明顺序。

## 🛡️ 编译期校验

EMG 的处理流程为：

```text
Entity and annotations
  -> shared source metadata
  -> V1/V2 metadata
  -> full-round validation
  -> Java source generation
  -> compilation of generated sources
```

Processor 会在调用 Filer 前校验非 class 输入、非法或空名称、缺失 group、重复目标 FQN、已有源码目标冲突、不支持类型和 Converter getter。任一候选失败都会阻止该轮全部生成，避免只留下部分源码。

类型解析基于编译期 `TypeMirror`，支持 primitive、声明类型、数组、参数化类型、通配符和嵌套类型。同简单名类型冲突时会回退到完整限定名。`TYPEVAR`、`ERROR`、`INTERSECTION`、`UNION`、`NONE` 和 `NULL` 等不支持类型会产生可定位的编译诊断。

V1 和 V2 都只读取来源类直接声明的非 `static` 字段，不收集继承字段。不要手改生成源码。业务扩展应使用继承、组合或手写专属类型。

View 输出带有内部 CLASS retention 的 `EmgGenerated` 标记（generator、source entity、View identity、schema fingerprint），并在编译器 `CLASS_OUTPUT` 中保存 ownership receipt。标记使用已有 annotations 构件，不增加运行时依赖。仅有 dependency 内的标记不能获得复用权限。真实非 clean javac 执行时，只有本地历史 View 的 fingerprint 和实际字段/accessor 签名均匹配当前模型，才允许复用；用户/dependency 同名冲突仍会失败。

增量重建替换 `CLASS_OUTPUT` 但保留生成源码输入时，只有 marker 匹配且编译器给出的源码文件身份与当前 `SOURCE_OUTPUT` 的目标资源完全一致，才允许恢复归属。通过 schema 与结构校验后，EMG 重建 class-output receipt。用户源码中复制的 marker、dependency class 和损坏 receipt 均不满足条件。只有 class 且缺少 receipt 时仍需 clean regeneration；此恢复依赖 JDK Trees API，不支持该 API 的编译器保持 receipt-only 策略。

所选字段新增、删除、类型或顺序变化时，编译报告 `EMG generated type is stale; clean regeneration is required`。执行 `mvn clean compile` 后重新生成。从 1.2.0 升级时，没有 provenance 的历史输出也需要一次 clean 构建；Processor 不能安全猜测其归属。`emg-annotations` 与 `emg-processor` 版本应保持一致。该策略覆盖 View，不承诺 V1 Converter 增量再生成。删除或重命名 View 声明后，旧输出可能残留到下一次 clean 构建。

## 🎯 Demo 示例

[`emg-demo`](emg-demo/) 中的 [`UserEntity`](emg-demo/src/main/java/io/github/forgottenlab/emg/demo/user/entity/UserEntity.java) 同时使用 `@AutoModel`、两个 `@AutoView`、ignore、alias 和多 group 字段。

完整 reactor 编译会生成并编译：

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`
- `UserBasicView`
- `UserProfileView`

Demo 还展示了在 service/controller 风格代码中使用生成类型，以及通过手写 `UserLoginResponse` 扩展基础 Response。它不依赖 Spring 或真实 MyBatis。Request 仍然手写，因为接口参数、校验和业务语义不能可靠地仅从 Entity 推导。

## 🧪 测试

当前 Processor 测试基线为 81 tests：

| 测试类 | 数量 | 范围 |
| --- | ---: | --- |
| `AutoModelProcessorCompilationTest` | 14 | V1 生成、开关、ignore、alias、多实体与重复编译 |
| `AutoModelProcessorHardeningTest` | 15 | 类型、字段边界、命名、冲突和 getter |
| `AutoModelProcessorSpiTest` | 3 | SPI、ServiceLoader 和 supported annotation types |
| `AutoViewProcessorCompilationTest` | 20 | V2 成功场景、组合、类型、顺序和 retention |
| `AutoViewProcessorValidationTest` | 10 | V2 诊断、冲突与零部分输出 |
| `AutoViewIncrementalCompilationTest` | 19 | 强制磁盘 javac、保留源码/class、更换 class-output 生命周期、provenance、真实冲突、过期 schema 与 Filer 实验 |

Processor 回归使用真实 `javac`/APT/SPI 路径，包含 touch 未变化 Entity 后显式调用 javac 的非 clean 场景。项目也提供独立消费者和隔离 Maven repository 核验方式。

annotation processor fixture 使用通用 `ProductEntity`，仅用于验证协作能力，不代表任何真实应用的字段模型。一次 `mvn clean compile` 中，Lombok 提供源类型 accessor，EMG 生成 `ProductAiView`，MapStruct 生成并编译 `ProductMapperImpl`。EMG-first 与 Lombok-first 两个 processor path profile 均已验证，正确性不依赖其中任何顺序。`lombok-mapstruct-binding` 只协调 Lombok 与 MapStruct，不控制 EMG 执行顺序。

从仓库根目录运行：

```bash
mvn clean test
```

在本地安装 EMG 后，可用以下命令复现独立 consumer 检查：

```bash
mvn -f integration-fixtures/plain-emg/pom.xml clean compile
mvn -f integration-fixtures/annotation-processors/pom.xml -Porder-emg-first clean compile
mvn -f integration-fixtures/annotation-processors/pom.xml -Porder-lombok-first clean compile
```

此结果只证明当前 Java 17 + Maven 范围，不扩展为 Gradle、JDK 21 或发布就绪声明。

## 🛠️ 常见问题与排查

### 没有生成任何类

检查普通 annotations 依赖和 `annotationProcessorPaths` 是否都存在，确认 `mvn -version` 使用 JDK 17，然后执行 `mvn clean compile`。来源必须是 class，并带有 `@AutoModel` 或 `@AutoView`；仅声明 `@ViewGroups` 不会触发生成。

### 提示 group 不存在或无效

`@AutoView("basic")` 至少需要一个直接声明的非 `static` 字段带有 `@ViewGroups("basic")`。group 不自动 trim，因此 `" basic"` 和 `"basic "` 都是非法值。

### Converter 报告 getter 缺失

当 `generateConverter = true` 时，每个进入已启用目标的字段都需要匹配的 public 实例零参 getter。检查访问级别、参数、返回类型，以及 primitive `boolean` 与包装 `Boolean` 的命名差异。

### IDEA 暂时看不到生成类型

先以 `mvn clean compile` 结果为准，再检查 `target/generated-sources/annotations`。部分 IDEA 版本或导入方式可能需要将该目录标记为 Generated Sources Root；这不影响 Maven 构建。

### `@ResponseAlias` 为什么不影响 View

它只控制 V1 Response 和 Converter 映射。View 成员由 `@ViewGroups` 独立决定，并保留实体原字段名。

### 能否从 Maven Central 直接下载

当前不能。请先在 EMG 仓库执行 `mvn clean install`，再让消费者使用本地 Maven repository 中的 `1.2.0` 构件。

## ⚠️ 当前限制

- `1.2.0` 尚未发布到 Maven Central，使用前需要本地执行 EMG 的 `mvn clean install`。
- 尚未发布稳定 Release，也不声称 production-ready。
- 只正式验证 Java 17 和 Maven；未验证 Gradle，也未承诺 JDK 21 正式兼容。
- 不支持 JoinView、View Converter 或 SQL 解析。
- 不支持配置自定义输出包；View 固定使用 `model.view` 约定。
- 只收集来源类直接声明的非 `static` 字段，不收集继承字段。
- 尚未验证跨模块读取已编译 `@ViewGroups` CLASS retention 元数据。
- Request 不自动生成；复杂联表、聚合结果和复杂 Projection 仍应手写。
- View 不生成构造器、builder、record 或 Lombok 代码。

## 🗺️ 路线图

V1 标准模型生成和 V2 字段分组单表 View 已实现。仓库现已包含 Apache-2.0 许可证、开源 POM 元数据、独立兼容性 fixture 与 CI 定义。Integration Ready 尚余 hosted GitHub Actions 成功运行这一项门禁。

路线图不是发布承诺。JoinView、SQL、自定义包、Gradle 支持、JDK 21 支持和 Maven Central 发布均未在当前版本中实现或承诺；任何后续功能都需要单独设计、验证和授权。

## 🤝 贡献与反馈

欢迎通过 [GitHub Issues](https://github.com/forgottenlab/entity-model-generator/issues) 报告可复现问题或提出聚焦建议。提交修改时，请保持范围小、兼容现有 V1/V2 API，并同步相关测试和中英文 README。

项目源于实际业务中反复维护 DTO、Response 和 Converter 的痛点，也感谢在学习、使用和反馈过程中提供帮助的人。

## 📜 许可证

本项目采用 [Apache License 2.0](LICENSE)。
