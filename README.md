# Entity Model Generator（EMG）

> 🚀 一个基于 **Java APT（Annotation Processing Tool）** 的编译期模型生成工具  
> 📦 目标是从 **Entity** 自动生成常见的 **DTO / BaseResponse / ListResponse / Converter**  
> ✨ 用更少的重复代码，换更清晰的分层与更稳定的维护体验

---

## 🔥 项目简介

在日常 Java 后端开发中，我们经常会围绕数据库实体类不断派生出一系列结构高度相似的类：

- `Entity`
- `DTO`
- `BaseResponse`
- `ListResponse`
- `Converter`
- 以及某些业务扩展 Response

这些类往往：

- 字段极其相似
- getter / setter 大量重复
- 只是在少数字段上存在“忽略”“裁剪”“重命名”的差异
- 一旦实体字段变更，需要同步修改很多类

也就是说：

> **明明这些类本质上都来源于同一个实体表结构，但开发者却要反复手工编写和维护。**

这正是 **Entity Model Generator（EMG）** 想解决的问题。

---

## 🧠 初衷：为什么会做这个项目？

这个项目并不是“为了造一个看起来很高级的工具”而出现的。

它真正的起点很朴素：

> **我在写业务代码时，发现大量类本质上都由实体表演变而来，但却仍要不断手写，冗余度非常高。**

例如，一个 `UserEntity` 很容易继续派生出：

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`
- `UserLoginResponse`（业务扩展）

而这些类之间很多时候只是：

- `password` 不该出现在 DTO / Response
- `nickname` 在 Response 中要改名为 `name`
- `createTime` / `updateTime` 不该出现在列表页返回中
- `Converter` 只是做机械字段拷贝

于是问题就来了：

### 如果继续纯手写，会发生什么？

- 样板代码越来越多
- 修改成本越来越高
- 很容易漏改字段
- 代码审阅价值越来越低
- 开发者时间被浪费在“重复劳动”上

所以我想做一件事：

> **让开发者只维护实体类，再通过编译期自动生成那些“高度相似、规则清晰”的模型。**

---

## ✅ 这个项目解决什么问题？

EMG 主要解决的是：

### 1. Entity 衍生模型重复编写问题
从一个实体派生出多个模型时，不必每次都重复手写。

### 2. 字段裁剪与重命名问题
通过注解描述字段规则，而不是在多个类里手工维护差异。

### 3. Converter 机械转换问题
自动生成基础转换器，减少重复映射代码。

### 4. 模型一致性问题
以实体为源，保证派生模型与实体字段变化保持同步。

---

## 🧩 解决思路

EMG 采用的是：

> **编译期生成，而不是运行期拼装。**

也就是说，它不是在程序运行时动态创建对象，而是在 `javac` 编译阶段，扫描实体类和注解信息，直接生成标准 Java 源码。

生成后的类是真实存在的 `.java` 文件，因此：

- IDE 可识别
- 可以调试
- 可以静态检查
- 不依赖反射
- 没有运行期开销

---

## 🚀 Quick Start

最小接入只需要修改两个位置：消费者项目的 `pom.xml`，以及一个作为生成来源的 Java class。Maven 配置只有两段：annotations 普通依赖和 processor path。

### 1. 前置条件

- JDK 17
- Maven

当前 `1.2.0` 尚未发布到 Maven Central。首次使用前需要先克隆 EMG，并将当前源码安装到本地 Maven repository：

```bash
git clone <EMG repository URL>
cd entity-model-generator
mvn clean install
```

### 2. 在消费者项目中配置 Maven

添加公开注解依赖：

```xml
<dependency>
    <groupId>io.github.forgottenlab.emg</groupId>
    <artifactId>emg-annotations</artifactId>
    <version>1.2.0</version>
</dependency>
```

再在 `maven-compiler-plugin` 中配置 annotation processor：

```xml
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
```

消费者不需要把 `emg-core` 声明为普通依赖；processor 会通过自身 Maven 依赖获得它。

### 3. 写一个最小 View 来源

```java
package com.example.user.entity;

import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.annotations.ViewGroups;

@AutoView("basic")
public class UserEntity {

    @ViewGroups("basic")
    private Long id;

    @ViewGroups("basic")
    private String username;
}
```

### 4. 编译

```bash
mvn clean compile
```

Maven 会自动加载 processor、编译生成源码并将其加入本次 javac。默认生成位置是：

```text
target/generated-sources/annotations
```

上例默认生成：

```text
com.example.user.model.view.UserBasicView
```

默认命名为 `BaseName + CapitalizedGroup + View`。`@AutoView` 可以独立使用；需要同时生成标准模型时，组合 `@AutoModel("User")` 即可。类名覆盖是可选项：

```java
@AutoModel("User")
@AutoView("basic")
@AutoView(value = "detail", name = "UserProfileView")
public class UserEntity {
    // fields and public getters required by the V1 Converter
}
```

不需要 Spring、MyBatis、Lombok、运行时反射、额外配置文件、手工 `-s` 或手工注册 generated-sources。Maven 构建不依赖 IDEA。

---

## 🏗️ 当前项目结构

```text
entity-model-generator
├── emg-annotations
│   └── 注解定义模块
│
├── emg-core
│   └── 核心元数据、常量、工具模块
│
├── emg-processor
│   └── APT 处理器模块
│
└── emg-demo
    └── 示例模块
```

---

## 📦 模块说明

### 1️⃣ `emg-annotations`

注解定义模块。

当前包含：

- `@AutoModel`
- `@AutoView`
- `@AutoViews`
- `@ViewGroups`
- `@DtoIgnore`
- `@ResponseIgnore`
- `@ListIgnore`
- `@ResponseAlias`

这一模块尽量保持轻量，只负责定义规则，不参与具体生成逻辑。

---

### 2️⃣ `emg-core`

核心元数据与工具模块。

主要包括：

- `AutoModelMetadata`
- `AutoViewMetadata`
- `SourceMetadata`
- `FieldMetadata`
- `ModelConstants`
- `NameUtils`
- `TypeUtils`

职责是为 processor 提供统一的元数据描述和基础工具支持。

---

### 3️⃣ `emg-processor`

APT 处理器模块，是项目的核心。

主要职责：

- 扫描 `@AutoModel`、`@AutoView` 和 `@AutoViews`
- 解析实体字段和注解
- 执行编译期校验
- 生成 DTO / Response / Converter / View

核心入口类：

```java
AutoModelProcessor
```

---

### 4️⃣ `emg-demo`

示例模块，用于演示：

- 如何定义实体类
- 如何触发生成
- 如何在 `service / controller` 中使用生成类
- 如何通过继承生成类扩展业务专属返回对象

---

## 📝 示例

### 定义实体类

```java
package io.github.forgottenlab.emg.demo.user.entity;

import io.github.forgottenlab.emg.annotations.*;

import java.time.LocalDateTime;

@AutoModel(
        value = "User",
        generateDto = true,
        generateBaseResponse = true,
        generateListResponse = true,
        generateConverter = true
)
public class UserEntity {

    private Long id;

    private String username;

    @DtoIgnore
    @ResponseIgnore
    private String password;

    @ResponseAlias("name")
    private String nickname;

    private Integer status;

    @ListIgnore
    private LocalDateTime createTime;

    @ListIgnore
    private LocalDateTime updateTime;
}
```

### 编译后自动生成

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

### 业务扩展示例

```java
public class UserLoginResponse extends UserBaseResponse {

    private String token;
    private Long expireTime;
}
```

这也是 EMG 当前推荐的返回模型扩展方式：

> **自动生成基础模型，手写业务专属扩展模型。**

---

## 📘 注解说明

### `@AutoModel`
标记实体类参与编译期模型生成。

可控制：

- 是否生成 DTO
- 是否生成 BaseResponse
- 是否生成 ListResponse
- 是否生成 Converter

---

### `@DtoIgnore`
当前字段不进入 DTO。

---

### `@ResponseIgnore`
当前字段不进入所有 Response。

---

### `@ListIgnore`
当前字段不进入 `ListResponse`。

---

### `@ResponseAlias("xxx")`
指定字段在 Response 中使用新的字段名。

例如：

```java
@ResponseAlias("name")
private String nickname;
```

会在 Response 中生成：

```java
private String name;
```

---

## 🧭 V2 自定义单表 View

V2 延续“约定优于配置”：常见场景只需要一个 group 名称，不需要重复罗列字段、配置包路径或指定默认类名。

### 最短写法

```java
@AutoView("basic")
public class UserEntity {

    @ViewGroups("basic")
    private Long id;

    @ViewGroups("basic")
    private String username;
}
```

编译后默认生成：

```text
model.view.UserBasicView
```

默认命名规则是：

```text
BaseName + CapitalizedGroup + View
```

`UserEntity` 会移除 `Entity` 后缀，因此 group 为 `basic` 时生成 `UserBasicView`。

### 一个字段属于多个分组

```java
@AutoView("basic")
@AutoView(value = "detail", name = "UserProfileView")
public class UserEntity {

    @ViewGroups({"basic", "detail"})
    private Long id;

    @ViewGroups("basic")
    private String username;

    @ViewGroups("detail")
    private String phone;
}
```

这里会生成：

- `model.view.UserBasicView`
- `model.view.UserProfileView`

`name` 是可选的完整类简单名覆盖；未填写时始终使用默认命名。

### 与 `@AutoModel` 组合

```java
@AutoModel("User")
@AutoView("basic")
public class UserEntity {
    // fields
}
```

- 只有 `@AutoModel`：继续生成 DTO、BaseResponse、ListResponse、Converter。
- 只有 `@AutoView`：无需额外声明 `@AutoModel`，只生成指定 View。
- 两者同时存在：V1 标准模型和 V2 View 同时生成。

View 字段只由 `@ViewGroups` 决定。`@DtoIgnore`、`@ResponseIgnore`、`@ListIgnore` 和 `@ResponseAlias` 不影响 View，View 中仍使用实体原字段名。

`@ViewGroups` 使用 `RetentionPolicy.CLASS`，为后续跨模块元数据读取保留能力；当前版本仍只处理本次 compilation 中的单实体 View。

### 当前边界

- 只支持单实体直接声明的非 `static` 字段，不收集继承字段。
- 不生成 View Converter。
- 不支持 JoinView 或 SQL 解析。
- 不提供包路径配置，统一生成到 `model.view`。
- 不生成构造器、builder、record 或 Lombok 代码。

---

## 🧪 编译时工作流程

EMG 的工作过程大致如下：

```text
Entity
  ↓
@AutoModel / @AutoView 扫描
  ↓
解析字段与注解
  ↓
构建共享 SourceMetadata 与 V1/V2 元数据
  ↓
整轮编译期预检
  ↓
生成 DTO / Response / Converter / View 源码
```

生成源码默认位于：

```text
target/generated-sources/annotations
```

---

## ❓ 常见问题

### 没有生成任何类

依次检查：

1. `pom.xml` 是否配置了 `annotationProcessorPaths`。
2. `mvn -version` 显示的 Java 是否为 JDK 17。
3. 是否执行了 `mvn clean compile`。
4. `@AutoModel` 或 `@AutoView` 是否标注在 class 上。
5. View 字段是否声明了与 `@AutoView` 一致的 `@ViewGroups`。

### 提示 group 不存在

`@AutoView("basic")` 要求至少一个直接声明的非 `static` 字段属于 `basic`：

```java
@ViewGroups("basic")
private Long id;
```

group 不会自动 trim；`" basic"` 和 `"basic "` 都是非法配置。

### IDEA 暂时看不到生成类

先执行 `mvn clean compile`，再检查 `target/generated-sources/annotations`。部分 IDEA 版本或导入方式可能需要把该目录标记为 Generated Sources Root。IDE 显示不是构建成功的判断依据，Maven 构建结果才是最终真相来源。

### `ResponseAlias` 为什么不影响 View

`@ResponseAlias` 只负责 V1 Response 字段命名。View 是否包含字段只由 `@ViewGroups` 决定，字段名保持实体原名。

### 能否修改 View 输出包

当前不支持。View 固定生成到 `model.view`，采用约定优于配置。

### 是否支持 JoinView

当前不支持。现阶段只生成单实体 View，也不解析 SQL。

### 是否可以直接从 Maven Central 获取

当前 `1.2.0` 尚未发布到 Maven Central。请先在 EMG 仓库执行 `mvn clean install`，再由消费者项目使用本地 Maven repository 中的构件。

---

## ⚠️ 使用注意事项（非常重要）

### 1. 当前版本以 **单表实体派生模型** 为核心
也就是说，EMG 当前最适合：

- DTO
- 详情 Response
- 列表 Response
- 基础 Converter

对于复杂联表结果、聚合结果、多表 DTO，目前仍建议手写。

---

### 2. Request 当前不自动生成
这是一个刻意保留的边界。

因为 Request 通常：

- 强依赖业务语义
- 需要和接口参数、校验规则耦合
- 很难只凭实体类准确推断

所以当前版本建议：

> **Request 手写，DTO / Response / Converter 自动生成。**

---

### 3. 不要手改生成代码
生成类是编译产物，下次编译会覆盖。

如果需要扩展，请通过：

- 继承
- 组合
- 手写业务扩展类

来完成。

---

### 4. IntelliJ IDEA 只负责显示辅助

EMG 不依赖 IDEA 才能生成代码。先运行 `mvn clean compile`；如果 Maven 已成功而 IDE 暂时无法解析生成类，再检查 annotation processing 设置，或把 `target/generated-sources/annotations` 标记为 Generated Sources Root。不同 IDEA 版本的自动识别行为可能不同。

---

### 5. Maven 会在同一次编译中处理生成源码

`emg-demo` 中的手写代码会直接依赖 APT 生成的类，例如：

- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

使用上述 `maven-compiler-plugin` 配置后，标准 `mvn clean compile` 会加载 processor、生成源码并在同一次构建中编译它们，不需要手工增加 `-s` 或 generated-sources 路径。

---

## 🧱 当前版本的特点

### 优点

- 编译期生成，运行期零额外开销
- 模型规则清晰
- 字段控制能力足够覆盖高频场景
- 生成代码真实可见、可调试
- 适合作为个人工具库持续演进

### 当前局限

- 主要支持单表模型派生
- Request 暂不自动生成
- 复杂 Projection 仍需手写
- View Converter 和 JoinView 当前不支持
- IDEA 的生成源码显示可能需要按版本手工标记，但不影响 Maven 构建

---

## 🚧 后续版本演化计划

### V1.1
目标：增强字段能力与注释能力

计划考虑：

- `@FieldComment`
- 更丰富的字段别名控制
- 更明确的生成类注释信息

---

### V1.2
目标：增强生成类型

计划考虑：

- 更细粒度的 Response 生成
- 更完整的 Converter 能力
- 列表 / 集合转换辅助方法增强

---

### V2
状态：已实现字段分组和自定义单表 View

当前能力：

- 使用 `@ViewGroups` 声明字段分组
- 使用 `@AutoView` 显式生成单表 View
- 保持编译期生成，不解析 SQL

---

### V3
目标：与其他工具链结合

可能方向包括：

- 与 MyBatis-Plus 二次封装工具结合
- 更标准的本地 Maven 使用方式
- 更完整的 starter 化支持

---

## 🌱 这个项目的定位

EMG 并不想成为一个“自动理解一切业务语义的万能代码生成器”。

它更希望成为一个：

- 边界清晰
- 规则明确
- 易于理解
- 易于扩展
- 适合长期迭代

的 **实体派生模型生成工具**。

换句话说：

> 它不是为了取代开发者，而是为了替开发者承担那些“明知重复、却不得不一遍遍写”的工作。

---

## 🙏 致谢

感谢一路上帮助过我的老师、家人和自己。

这个项目并不是凭空出现的，它来自于：

- 学习过程中的积累
- 实际编码中的痛点
- 一次次踩坑后的总结
- 对“让代码更好写一点”的朴素愿望

---

## ✨ 结语

如果你也曾在项目中反复编写：

- DTO
- Response
- Converter

并且觉得：

> “这些明明都只是从实体表结构演变而来，为什么还要一遍遍手写？”

那么，EMG 就是我给出的一个编译期答案。

欢迎一起交流、改进与共建。
