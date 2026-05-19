# Monodula

> 模块化单体工具链 —— **零代码改动**即可拆分服务。

[English](README.md)

## 为什么选择模块化单体？

微服务受到了很多关注，但它带来了严重的运维负担：分布式事务、网络延迟、服务发现、独立部署流水线，以及跨进程边界的调试。对大多数团队和产品而言——尤其是早期和成长阶段——这些复杂性并不值得。

这并不是新观点。2015 年，Martin Fowler 在 [MonolithFirst](https://martinfowler.com/bliki/MonolithFirst.html) 中写道：

> "不要用微服务启动新项目，哪怕你确信项目会足够大。要仔细地设计单体，注重软件内部的模块化。"

《Building Microservices》和《Monolith to Microservices》的作者 Sam Newman 也持相同立场：结构良好的模块化单体本身就是一种合理的架构，而不仅仅是通往微服务的垫脚石。

### 真实验证：Shopify

Shopify 的核心 Rails 应用增长到超过 **280 万行代码**。他们没有拆分成微服务，而是选择继续在单体上投入——但重点加强了模块边界的约束。他们的方案：[Rails Engines](https://shopify.engineering/deconstructing-monolith-designing-software-maximizes-developer-productivity) 做结构划分，[Packwerk](https://github.com/Shopify/packwerk) 做静态依赖分析，Sorbet 做模块间的类型化接口。

他们的目标，用自己的话说：

> *"开发者应该感觉自己在开发一个比实际小得多的应用。"*

### 现有工具

| 工具 | 生态 | 方式 |
|------|------|------|
| [Spring Modulith](https://spring.io/projects/spring-modulith) | Java / Spring Boot | 包级别模块边界、事件发布日志、文档生成 |
| [Packwerk](https://github.com/Shopify/packwerk) | Ruby / Rails | 静态依赖分析与边界约束 |
| [ArchUnit](https://www.archunit.org/) | Java | 架构规则作为单元测试 —— `monodula-archunit` 内置 6 条开箱即用的规则（R001–R006），直接引入即可使用；R007 由 `monodula:check` 在 Maven 层面单独校验 |
| [kgrzybek/modular-monolith-with-ddd](https://github.com/kgrzybek/modular-monolith-with-ddd) | Java / DDD | 结合领域驱动设计的模块化单体参考实现 |

### 结论

模块化单体提供了一条中间路：
- **单一部署单元** —— 无分布式系统复杂性
- **进程内通信** —— 模块间无网络开销
- **清晰的模块边界** —— 代码组织方式如同独立服务
- **渐进式拆分** —— 当团队和流量真正需要时，再将个别模块独立部署

**你不必在"整洁的单体"和"未来的灵活性"之间二选一。** 一个结构良好的模块化单体可以兼得两者。

## 为什么需要 Monodula？

模块化单体是经过验证的架构模式。但大多数实现只关注"让单体保持整洁"——**没有人从第一天就为拆分那一天做设计。**

Monodula 的立场不同：**每个模块天生就准备好成为独立服务。** 架构在编译期强制模块边界，并提供一条命令将任意模块拆分为独立部署——无需改动一行业务代码。

但拆分并不是使用 Monodula 的唯一理由。

**即便你的项目永远不拆分，Monodula 每天都在发挥价值。**

在编译期强制模块边界意味着：
- 开发者无法意外地让 `payment-core` 依赖 `order-core`——构建立刻失败
- `module-api` 始终保持框架无关，极易在隔离环境中复用和测试
- 新成员上手更快——模块职责是物理约束，而不只是文档上的约定
- 重构和新功能开发更安全——你清楚地知道每个模块拥有什么、不拥有什么

大多数项目永远不会拆成微服务，这完全没问题。**清晰边界的纪律让你的单体更易维护、更易阅读、更易扩展——无论拆分是否发生。**

### 和 Spring Modulith 有什么区别？

| | Spring Modulith | Monodula |
|--|----------------|----------|
| 设计目标 | 让单体保持整洁 | **强制模块边界 + 从第一天就面向拆分** |
| 模块粒度 | 包级别（同一个项目内） | Maven 模块级别（物理拆分） |
| Endpoints 隔离 | 无此概念 | **`module-app` 层物理隔离** |
| 服务拆分 | 需要改代码 | **零代码改动** |
| 边界约束 | package-private + ArchUnit | Maven 依赖（编译期）+ **ArchUnit 规则（测试期）** |

## 核心架构

### 模块结构

每个业务模块遵循统一的三层结构：

```
module-x/
├── module-x-api/      # 接口契约（DTO、模型、服务接口）
├── module-x-core/     # 业务实现（Service、Repository、Mapper）
└── module-x-app/      # 仅入口（Controller、Dubbo Provider、定时任务）
```

骨架生成的 `common` 模块采用四层结构：

```
common/
├── common-api/        # 跨模块共享的 DTO 和接口
├── common-core/       # 共享业务工具类和基础实现
├── common-infras/     # 基础设施配置（DB、缓存、RPC 等）
└── common-app/        # 公共入口配置（拦截器、过滤器等）
```

### 依赖规则

```
                    ┌──────────────┐
                    │  monolith-app│  （聚合层，无业务代码）
                    └──────┬───────┘
                           │ 依赖
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌───────────┐ ┌───────────┐ ┌───────────┐
        │module-a-app│ │module-b-app│ │module-c-app│
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │              │              │
              ▼              ▼              ▼
        ┌────────────┐ ┌────────────┐ ┌────────────┐
        │module-a-core│ │module-b-core│ │module-c-core│
        └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
              │              │              │
              ▼              ▼              ▼
        ┌────────────┐ ┌────────────┐ ┌────────────┐
        │module-a-api│ │module-b-api│ │module-c-api│
        └────────────┘ └────────────┘ └────────────┘

跨模块依赖：
  module-b-core ──依赖──▶ module-a-api （不是 module-a-core！）
```

**核心规则：**
- `module-x-core` 只能依赖其他模块的 `api`，绝不能依赖 `core` 或 `app`
- `module-x-api` 框架无关 —— 不依赖 Spring、不依赖 ORM
- `module-x-app` 只放入口 —— 不含业务逻辑

### `app` 层：零代码拆分的关键

`module-x-app` 层是**入口污染层**。它包含的是——当其他模块独立部署时不应该运行的东西：

- REST Controller
- Dubbo Provider
- MQ 消费者
- 定时任务

拆分模块时，只需**不依赖其他模块的 `app`**——它们的 endpoints 不在 classpath 上，Spring 组件扫描自然扫不到。不需要任何排除配置。

### 模块自治

每个模块完全自包含、自注册。**增删模块，永远不需要手动修改任何中心化配置文件。**

这一机制在两个层面实现：

**Maven 层 —— 自动维护 pom.xml**

`monodula:add` 自动将新模块的 `-app` 依赖追加到 `monolith-application/pom.xml`，无需开发者手动编辑。

**Spring Boot 层 —— 通过 `spring.factories` 自加载配置**

每个模块的 `-core` jar 中内置 `META-INF/spring.factories`，注册自己的 `EnvironmentPostProcessor`：

```
# payment-core/src/main/resources/META-INF/spring.factories
org.springframework.boot.env.EnvironmentPostProcessor=\
  com.example.myproject.payment.config.PaymentConfigLoader
```

Spring Boot 启动时从 classpath 自动发现该文件，调用 `PaymentConfigLoader` 加载模块自己的配置文件（`payment-config.yml`、`payment-config-dev.yml` 等）——主应用类无需任何 `@Import` 或扫描包配置。

最终效果：从 classpath 移除一个模块的 `-app` jar，该模块的所有 Controller、定时任务、MQ 消费者和配置全部消失；加回来则全部恢复。主应用类永远不需要改动。

## 服务拆分

### 单体部署

```xml
<!-- monolith-application/pom.xml -->
<dependencies>
    <dependency><artifactId>module-a-app</artifactId></dependency>
    <dependency><artifactId>module-b-app</artifactId></dependency>
    <dependency><artifactId>module-c-app</artifactId></dependency>
    <dependency><artifactId>common-app</artifactId></dependency>
</dependencies>
```

### 独立部署（将 module-b 拆为独立服务）

```xml
<!-- module-b-standalone-application/pom.xml -->
<dependencies>
    <!-- 自己的入口 -->
    <dependency><artifactId>module-b-app</artifactId></dependency>

    <!-- 其他模块：api + core（进程内调用），不依赖 app -->
    <dependency><artifactId>module-a-api</artifactId></dependency>
    <dependency><artifactId>module-a-core</artifactId></dependency>

    <dependency><artifactId>common-app</artifactId></dependency>
</dependencies>
```

**效果：** module-b 独立运行时，module-a 的业务逻辑可同进程调用，但 module-a 的 Controller、定时任务、MQ 消费者完全不加载。零代码改动，只改 pom.xml + Dockerfile。

## 快速开始

### 1. 生成项目骨架

```bash
mvn archetype:generate \
  -DarchetypeGroupId=io.github.creek91 \
  -DarchetypeArtifactId=monodula-archetype \
  -DgroupId=com.example \
  -DartifactId=my-project \
  -Dmodules=order,payment,user \
  -DconfigCenter=apollo \
  -DjavaVersion=17 \
  -DspringBootVersion=3.5.0
```

生成包含 `order`、`payment`、`user` 三个业务模块的完整项目骨架，每个模块均包含 `api`、`core`、`app` 三个子模块。`monolith-application` 已内置预配置的 `ArchitectureTest.java`，开箱即用地校验六条 ArchUnit 规则（R001–R006），无需手动编写。

### 2. 新增业务模块

```bash
mvn monodula:add -Dmodule=notification
```

自动生成 `notification/`（含 `notification-api`、`notification-core`、`notification-app`），并自动完成：
- 在根 pom 的 `<modules>` 中注册 `<module>notification</module>`
- 在根 pom 的 `<dependencyManagement>` 中添加 `notification-{api,core,app}`
- 将 `notification-app` 添加为 `monolith-application` 的依赖

模块名须满足 `[a-z][a-z0-9-]*`（小写字母、数字、连字符；首字符必须为字母，如 `payment`、`order-item`）。

### 3. 校验模块边界

```bash
mvn monodula:check
```

输出：
```
[ERROR] R001: module-b-core → module-a-core
[ERROR] R003: module-a-api → module-a-core
[INFO] Monodula boundary violations found: 2
```

`monodula:check` 扫描 reactor 中每个模块 `pom.xml` 的 `<dependencies>` 声明，仅校验项目内部模块之间的依赖，第三方库被忽略。

| 规则 | 来源模块 | 目标模块 | 约束说明 |
|------|---------|---------|---------|
| R001 | `*-core` | `*-core` | core 不能依赖其他业务模块的 core（common-core 除外） |
| R002 | `*-core` | `*-app`、`common-app` | core 不能依赖任何 app 层 |
| R003 | `*-api` | `*-core`、`common-core` | api 不能依赖任何 core 层 |
| R004 | `*-api` | `*-app`、`common-app` | api 不能依赖任何 app 层 |
| R005 | `common-*` | 业务模块 | common 不能依赖业务模块 |
| R006 | `*-app` | `*-app` | app 不能依赖其他业务模块的 app（common-app 除外） |
| R007 ¹ | 任意 | 自身 | 模块不能在 pom.xml 中声明对自己的依赖 |

¹ R007 仅由 `monodula:check`（Maven pom.xml 层面）校验，`monodula-archunit` 中没有对应的 ArchUnit 规则。

所有违规均为 `ERROR` 级别，会导致构建失败。

### 4. 拆分模块为独立服务

```bash
mvn monodula:split -Dmodule=module-b
```

自动完成：
1. 解析模块依赖树
2. 生成 `application/module-b-application/`，含 pom.xml 和 Dockerfile
3. 更新 `monolith-application/pom.xml`：将 `module-b-app` 替换为 `module-b-core`
4. 更新 `application/pom.xml`，注册新的独立应用模块
5. 在新独立应用的测试源码中生成 `ArchitectureTest.java`，校验其 classpath 上的 R001–R006 规则 ²

² 生成的 `ArchitectureTest.java` 涵盖六条 ArchUnit 规则（R001–R006）。R007（自依赖）没有对应的 ArchUnit 规则，仅由 `monodula:check` 校验。

### 5. 移除业务模块

```bash
# 仅移除 pom 引用，保留源码目录
mvn monodula:remove -Dmodule=notification

# 移除 pom 引用并删除源码目录
mvn monodula:remove -Dmodule=notification -Dpurge=true
```

安全地将业务模块从项目中移除，执行流程：
1. 校验没有其他模块依赖 `notification-api`、`notification-core` 或 `notification-app`——若存在依赖方，直接报错并列出全部依赖方
2. 从根 pom 的 `<modules>` 中移除 `<module>notification</module>`
3. 从根 pom 的 `<dependencyManagement>` 中移除 `notification-{api,core,app}`
4. 从 `monolith-application/pom.xml` 的 `<dependencies>` 中移除 `notification-app`（若文件存在）
5. 递归删除 `notification/` 源码目录（仅当 `purge=true` 时执行）

## 项目模块

| 模块 | 说明 |
|------|------|
| `monodula-archetype` | Maven Archetype 项目骨架生成 |
| `monodula-maven-plugin` | Maven 插件 —— `add`、`check`、`split`、`remove` 四个 goal |
| `monodula-archunit` | 预置 ArchUnit 模块边界校验规则 |

## ArchUnit 集成

`monodula-archunit` 内置 6 条 ArchUnit 规则（R001–R006），在测试期强制校验 Monodula 的依赖契约。将其加入任意需要校验整体架构的模块（通常是专用的 `architecture-test` 模块，或根模块的 test scope）。

> **说明：** R007（模块不能依赖自身）在 Maven pom.xml 层面由 `monodula:check` 校验，`monodula-archunit` 中没有对应的 ArchUnit 规则。

### 添加依赖

```xml
<dependency>
    <groupId>io.github.creek91</groupId>
    <artifactId>monodula-archunit</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### 编写架构测试

```java
import com.github.monodula.archunit.MonodulaRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.example.myproject",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    // R001：core 不能依赖其他模块的 core
    @ArchTest
    ArchRule r001 = MonodulaRules.coreNotDependOnOtherCore();

    // R002：core 不能依赖其他模块的 app
    @ArchTest
    ArchRule r002 = MonodulaRules.coreNotDependOnOtherApp();

    // R003：api 不能依赖任何 core
    @ArchTest
    ArchRule r003 = MonodulaRules.apiNotDependOnCore();

    // R004：api 不能依赖任何 app
    @ArchTest
    ArchRule r004 = MonodulaRules.apiNotDependOnApp();

    // R005：common 不能依赖业务模块
    @ArchTest
    ArchRule r005 = MonodulaRules.commonNotDependOnBusiness();

    // R006：app 不能依赖其他模块的 app
    @ArchTest
    ArchRule r006 = MonodulaRules.appNotDependOnOtherApp();
}
```

### 规则说明

| 规则 | 约束内容 |
|------|---------|
| R001 `coreNotDependOnOtherCore` | `*.core.*` 不得引用其他模块的 `*.core.*` 类（common-core 除外） |
| R002 `coreNotDependOnOtherApp` | `*.core.*` 不得引用任何模块的 `*.app.*` 类 |
| R003 `apiNotDependOnCore` | `*.api.*` 不得引用任何 `*.core.*` 类 |
| R004 `apiNotDependOnApp` | `*.api.*` 不得引用任何 `*.app.*` 类 |
| R005 `commonNotDependOnBusiness` | `*.common.*` 不得引用业务模块类 |
| R006 `appNotDependOnOtherApp` | `*.app.*` 不得引用其他模块的 `*.app.*` 类（common-app 除外） |

这些规则是 `monodula:check`（校验 Maven pom.xml 中的依赖声明）的补充，用于捕获**实际 Java 代码层面**的违规——例如 pom.xml 无法感知的静态导入或反射引用。

## 设计原则

1. **面向拆分设计** —— 每个模块无需改代码即可独立部署
2. **编译期边界** —— Maven 依赖在编译期强制约束，而非仅靠约定
3. **Classpath 隔离** —— 不依赖 `module-x-app` = endpoints 自然不存在
4. **模块自治** —— 每个模块通过 `spring.factories` 自注册；增删模块无需修改任何中心化配置
5. **务实数据隔离** —— 共享数据库 + 表前缀约定（如 `t_module_a_xxx`、`t_module_b_xxx`）
6. **统一配置中心** —— 单个 Apollo 项目 + namespace 按模块隔离

## 环境要求

- Java 17+
- Maven 3.8+
- Spring Boot 3.x

## 许可证

[Apache 2.0](LICENSE)
