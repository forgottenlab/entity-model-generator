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

## ⚙️ 核心使用方式

你只需要在实体类上写一次：

```java
@AutoModel(
        value = "User",
        generateDto = true,
        generateBaseResponse = true,
        generateListResponse = true,
        generateConverter = true
)
public class UserEntity {
    ...
}
```

再配合字段级注解：

- `@DtoIgnore`
- `@ResponseIgnore`
- `@ListIgnore`
- `@ResponseAlias`

编译后自动生成：

- `UserDTO`
- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

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
- `FieldMetadata`
- `ModelConstants`
- `NameUtils`
- `TypeUtils`

职责是为 processor 提供统一的元数据描述和基础工具支持。

---

### 3️⃣ `emg-processor`

APT 处理器模块，是项目的核心。

主要职责：

- 扫描 `@AutoModel`
- 解析实体字段和注解
- 执行编译期校验
- 生成 DTO / Response / Converter

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
@AutoModel 扫描
  ↓
解析字段与注解
  ↓
构建元数据 AutoModelMetadata / FieldMetadata
  ↓
编译期校验
  ↓
生成 DTO / Response / Converter 源码
```

生成源码默认位于：

```text
target/generated-sources/annotations
```

---

## 🚀 使用方式

### 方式一：源码 / 多模块方式使用（当前推荐）

当前版本最适合：

- 直接克隆源码
- 以多模块方式学习与使用
- 便于理解 APT 工作流程
- 便于后续二次开发

### 方式二：本地 Maven 仓库使用

在项目根目录执行：

```bash
mvn clean install
```

之后可在其他项目中以本地依赖方式引入：

```xml
<dependency>
    <groupId>io.github.forgottenlab.emg</groupId>
    <artifactId>emg-annotations</artifactId>
    <version>1.2.0</version>
</dependency>

<dependency>
    <groupId>io.github.forgottenlab.emg</groupId>
    <artifactId>emg-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

并在 `maven-compiler-plugin` 中引入：

```xml
<annotationProcessorPaths>
    <path>
        <groupId>io.github.forgottenlab.emg</groupId>
        <artifactId>emg-processor</artifactId>
        <version>1.2.0</version>
    </path>
</annotationProcessorPaths>
```

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

### 4. IntelliJ IDEA 中的关键设置

如果你在 IDEA 中能看到生成代码，但手写类仍然报红，通常要检查下面两点：

#### 开启注解处理
建议只对 `emg-demo.main` 开启：

- 勾选 **启用注解处理**
- 选择 **从项目类路径获取处理器**

#### 标记生成源码目录
将：

```text
emg-demo/target/generated-sources/annotations
```

标记为：

```text
生成的源代码根目录
```

在 IDEA 中通常会显示为：

- 蓝色目录
- 带雪花图标

这一步非常关键。

---

### 5. Maven 编译阶段的一个坑

在当前项目里，`emg-demo` 中的手写代码会**直接依赖 APT 生成的类**，例如：

- `UserBaseResponse`
- `UserListResponse`
- `UserConverter`

因此编译链路必须保证：

> **生成代码先产生，再被后续编译识别。**

这也是当前 `emg-demo` 模块中最需要谨慎配置的部分。

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
- Maven/IDEA 对生成源码目录的处理需要配置好
- 示例模块对“编译时序”较敏感

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
目标：显式 Projection 生成

思路：

- 允许用户声明某些“投影模型”
- 仍保持编译期生成
- 不强行做 SQL 自动推断

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
