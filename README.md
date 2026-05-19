# Monodula

> A modular monolith toolkit that lets you split services with **zero code changes**.

[中文](README_ZH.md)

## Why Modular Monolith?

Microservices get a lot of attention, but they come with serious operational overhead: distributed transactions, network latency, service discovery, independent deployment pipelines, and debugging across process boundaries. For most teams and most products — especially in early and growth stages — this complexity is not justified.

This isn't a new observation. In 2015, Martin Fowler wrote [MonolithFirst](https://martinfowler.com/bliki/MonolithFirst.html):

> "You shouldn't start a new project with microservices, even if you're sure your application will be big enough to make it worthwhile. Design the monolith carefully, paying attention to modularity within the software."

Sam Newman, author of *Building Microservices* and *Monolith to Microservices*, echoes this: a well-structured modular monolith is a legitimate architecture in its own right, not just a stepping stone.

### Real-world validation: Shopify

Shopify's core Rails application grew to over **2.8 million lines of code**. Rather than splitting into microservices, they doubled down on the monolith — but invested heavily in enforcing module boundaries. Their approach: [Rails Engines](https://shopify.engineering/deconstructing-monolith-designing-software-maximizes-developer-productivity) for structural separation, [Packwerk](https://github.com/Shopify/packwerk) for static dependency analysis, and Sorbet for typed interfaces between modules.

Their goal, in their own words:

> *"Developers should feel like they are working on a much smaller app than they actually are."*

### Existing tools

| Tool | Ecosystem | Approach |
|------|-----------|----------|
| [Spring Modulith](https://spring.io/projects/spring-modulith) | Java / Spring Boot | Package-level module boundaries, event publication log, documentation generation |
| [Packwerk](https://github.com/Shopify/packwerk) | Ruby / Rails | Static dependency analysis and boundary enforcement |
| [ArchUnit](https://www.archunit.org/) | Java | Architecture rules as unit tests — `monodula-archunit` ships 6 pre-built rules (R001–R006) ready to drop into any project; R007 is enforced by `monodula:check` at the Maven level |
| [kgrzybek/modular-monolith-with-ddd](https://github.com/kgrzybek/modular-monolith-with-ddd) | Java / DDD | Reference implementation combining modular monolith with Domain-Driven Design |

### The bottom line

A modular monolith offers a middle ground:
- **Single deployment unit** — no distributed systems complexity
- **In-process communication** — no network overhead between modules
- **Clear module boundaries** — code is organized as if it were separate services
- **Incremental extraction** — individual modules can be split out when the team and traffic actually require it

**You don't have to choose between a clean monolith and future flexibility.** A well-structured modular monolith gives you both.

## Why Monodula?

Modular monolith is a proven architectural pattern. But most implementations only focus on keeping the monolith tidy — **no one designs for the day you actually need to split it.**

Monodula takes a different stance: **every module is born ready to be an independent service.** The architecture enforces module boundaries at compile time, and provides a one-command tool to split any module into a standalone deployment — without touching a single line of business code.

But splitting is not the only reason to use Monodula.

**Even if your project never splits, Monodula pays off every day.**

Enforcing module boundaries at compile time means:
- A developer cannot accidentally introduce a dependency from `payment-core` to `order-core` — the build fails immediately
- `module-api` stays framework-agnostic, making it trivially reusable and testable in isolation
- New team members understand the codebase faster because module responsibilities are physically enforced, not just documented
- Refactoring and feature additions are safer — you know exactly what each module owns and what it doesn't

Most projects never get split into microservices. That's fine. **The discipline of clear boundaries makes your monolith more maintainable, more readable, and easier to extend — regardless of whether a split ever happens.**

### How is it different from Spring Modulith?

| | Spring Modulith | Monodula |
|--|----------------|----------|
| Design goal | Keep the monolith clean | **Enforce boundaries + split-ready from day one** |
| Module granularity | Package-level (within one project) | Maven module-level (physical separation) |
| Endpoints isolation | No concept | **`module-app` layer is physically isolated** |
| Service splitting | Requires code changes | **Zero code changes** |
| Boundary enforcement | Package-private + ArchUnit | Maven dependency (compile-time) + **ArchUnit rules (test-time)** |

## Core Architecture

### Module Structure

Every business module follows the same three-layer structure:

```
module-x/
├── module-x-api/      # Interface contracts (DTO, models, service interfaces)
├── module-x-core/     # Business implementation (Service, Repository, Mapper)
└── module-x-app/      # Endpoints only (Controller, Dubbo Provider, Job)
```

The `common` module generated by the archetype has a four-layer structure:

```
common/
├── common-api/        # Shared DTOs and interfaces used across all modules
├── common-core/       # Shared business utilities and base implementations
├── common-infras/     # Infrastructure concerns (DB, cache, RPC config, etc.)
└── common-app/        # Common endpoint wiring (interceptors, filters, etc.)
```

### Dependency Rules

```
                    ┌──────────────┐
                    │  monolith-app│  (aggregator, no business code)
                    └──────┬───────┘
                           │ depends on
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

Cross-module dependency:
  module-b-core ──depends on──▶ module-a-api  (NOT module-a-core!)
```

**Key rules:**
- `module-x-core` can only depend on other modules' `api`, never `core` or `app`
- `module-x-api` is framework-agnostic — no Spring, no ORM
- `module-x-app` contains only endpoints — no business logic

### The `app` Layer: The Key to Zero-Code Splitting

The `module-x-app` layer is the **endpoints pollution layer**. It holds everything that should NOT run when another module is deployed independently:

- REST Controllers
- Dubbo Providers
- MQ Consumers
- Scheduled Jobs

When you split a module, you simply **don't depend on other modules' `app`** — their endpoints won't be on the classpath, and Spring's component scan will naturally skip them. No exclusion config needed.

### Module Autonomy

Every module is fully self-contained and self-registering. **Adding or removing a module never requires manually editing a central configuration file.**

This is implemented at two levels:

**Maven level — automatic pom.xml wiring**

`monodula:add` automatically appends the new module's `-app` dependency to `monolith-application/pom.xml`. No developer touches that file by hand.

**Spring Boot level — self-loading config via `spring.factories`**

Each module's `-core` jar ships a `META-INF/spring.factories` file that registers its own `EnvironmentPostProcessor`:

```
# payment-core/src/main/resources/META-INF/spring.factories
org.springframework.boot.env.EnvironmentPostProcessor=\
  com.example.myproject.payment.config.PaymentConfigLoader
```

Spring Boot discovers this file from the classpath at startup and invokes `PaymentConfigLoader`, which loads the module's own YAML files (`payment-config.yml`, `payment-config-dev.yml`, …) without any import in the main application class.

The result: dropping a module's `-app` jar from the classpath is enough to remove it entirely — its controllers, jobs, and config all disappear. Adding it back brings everything online. The monolith application class never needs to change.

## Service Splitting

### Monolithic Deployment

```xml
<!-- monolith-application/pom.xml -->
<dependencies>
    <dependency><artifactId>module-a-app</artifactId></dependency>
    <dependency><artifactId>module-b-app</artifactId></dependency>
    <dependency><artifactId>module-c-app</artifactId></dependency>
    <dependency><artifactId>common-app</artifactId></dependency>
</dependencies>
```

### Independent Deployment (module-b as a standalone service)

```xml
<!-- module-b-standalone-application/pom.xml -->
<dependencies>
    <!-- Own endpoints -->
    <dependency><artifactId>module-b-app</artifactId></dependency>

    <!-- Other modules: api + core (for in-process invocation), NOT app -->
    <dependency><artifactId>module-a-api</artifactId></dependency>
    <dependency><artifactId>module-a-core</artifactId></dependency>

    <dependency><artifactId>common-app</artifactId></dependency>
</dependencies>
```

**Result:** module-b runs with module-a's business logic available in-process, but module-a's Controllers, Jobs, and MQ Consumers are completely absent. Zero code changes. Only pom.xml + Dockerfile.

## Quick Start

### 1. Generate a project

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

This generates a full project skeleton with `order`, `payment`, and `user` modules, each with `api`, `core`, and `app` sub-modules. `monolith-application` also includes a pre-wired `ArchitectureTest.java` that enforces the six ArchUnit rules (R001–R006) out of the box — no manual setup required.

### 2. Add a new module

```bash
mvn monodula:add -Dmodule=notification
```

Scaffolds `notification/` (with `notification-api`, `notification-core`, `notification-app`), and automatically:
- Registers `<module>notification</module>` in the root pom's `<modules>`
- Adds `notification-{api,core,app}` to the root pom's `<dependencyManagement>`
- Adds `notification-app` as a dependency of `monolith-application`

Module name must match `[a-z][a-z0-9-]*` (lowercase letters, digits, hyphens; e.g. `payment`, `order-item`).

### 3. Check module boundaries

```bash
mvn monodula:check
```

Output:
```
[ERROR] R001: module-b-core → module-a-core
[ERROR] R003: module-a-api → module-a-core
[INFO] Monodula boundary violations found: 2
```

`monodula:check` scans `<dependencies>` declarations in every reactor module's `pom.xml`. Only intra-project dependencies are validated; third-party libraries are ignored.

| Rule | Source | Target | Constraint |
|------|--------|--------|------------|
| R001 | `*-core` | `*-core` | Core cannot depend on another module's core (common-core is excluded) |
| R002 | `*-core` | `*-app`, `common-app` | Core cannot depend on any app layer |
| R003 | `*-api` | `*-core`, `common-core` | Api cannot depend on any core layer |
| R004 | `*-api` | `*-app`, `common-app` | Api cannot depend on any app layer |
| R005 | `common-*` | business modules | Common cannot depend on business modules |
| R006 | `*-app` | `*-app` | App cannot depend on another module's app (common-app is excluded) |
| R007 ¹ | any | itself | Module cannot declare a dependency on itself |

¹ R007 is enforced by `monodula:check` (Maven pom.xml level) only. There is no ArchUnit counterpart in `monodula-archunit`.

All violations are `ERROR` severity and fail the build.

### 4. Split a module into an independent service

```bash
mvn monodula:split -Dmodule=module-b
```

Automatically:
1. Resolves the module's dependency tree
2. Generates `application/module-b-application/` with pom.xml and Dockerfile
3. Updates `monolith-application/pom.xml`: replaces `module-b-app` with `module-b-core`
4. Updates `application/pom.xml` to include the new standalone app
5. Generates `ArchitectureTest.java` in the new standalone app's test sources, enforcing R001–R006 for its own classpath ²

² The generated `ArchitectureTest.java` covers the six ArchUnit rules (R001–R006). R007 (self-dependency) has no ArchUnit counterpart and is only checked by `monodula:check`.

### 5. Remove a module

```bash
# Remove pom references only, keep source directory
mvn monodula:remove -Dmodule=notification

# Remove pom references and delete source directory
mvn monodula:remove -Dmodule=notification -Dpurge=true
```

Safely removes a business module from the project:
1. Validates that no other module depends on `notification-api`, `notification-core`, or `notification-app` — fails with a clear error listing all dependents if any are found
2. Removes `<module>notification</module>` from the root pom
3. Removes `notification-{api,core,app}` from the root pom's `<dependencyManagement>`
4. Removes `notification-app` from `monolith-application/pom.xml` (if it exists)
5. Deletes `notification/` recursively (only when `purge=true`)

## Project Modules

| Module | Description |
|--------|-------------|
| `monodula-archetype` | Maven Archetype for project scaffolding |
| `monodula-maven-plugin` | Maven plugin — `add`, `check`, `split`, `remove` goals |
| `monodula-archunit` | Pre-built ArchUnit rules for module boundary validation |

## ArchUnit Integration

`monodula-archunit` ships 6 pre-built ArchUnit rules (R001–R006) that enforce the Monodula dependency contract at test time. Add it to any module that should verify the whole project's architecture (typically a dedicated `architecture-test` module or your root module's test scope).

> **Note:** R007 (module cannot depend on itself) is enforced at the Maven pom.xml level by `monodula:check` and has no ArchUnit counterpart in `monodula-archunit`.

### Add the dependency

```xml
<dependency>
    <groupId>io.github.creek91</groupId>
    <artifactId>monodula-archunit</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### Write the architecture test

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

    // R001: core cannot depend on another module's core
    @ArchTest
    ArchRule r001 = MonodulaRules.coreNotDependOnOtherCore();

    // R002: core cannot depend on another module's app
    @ArchTest
    ArchRule r002 = MonodulaRules.coreNotDependOnOtherApp();

    // R003: api cannot depend on any core
    @ArchTest
    ArchRule r003 = MonodulaRules.apiNotDependOnCore();

    // R004: api cannot depend on any app
    @ArchTest
    ArchRule r004 = MonodulaRules.apiNotDependOnApp();

    // R005: common cannot depend on business modules
    @ArchTest
    ArchRule r005 = MonodulaRules.commonNotDependOnBusiness();

    // R006: app cannot depend on another module's app
    @ArchTest
    ArchRule r006 = MonodulaRules.appNotDependOnOtherApp();
}
```

### What each rule enforces

| Rule | Constraint |
|------|-----------|
| R001 `coreNotDependOnOtherCore` | `*.core.*` must not import classes from any other module's `*.core.*` (common-core is allowed) |
| R002 `coreNotDependOnOtherApp` | `*.core.*` must not import classes from any module's `*.app.*` |
| R003 `apiNotDependOnCore` | `*.api.*` must not import classes from any `*.core.*` |
| R004 `apiNotDependOnApp` | `*.api.*` must not import classes from any `*.app.*` |
| R005 `commonNotDependOnBusiness` | `*.common.*` must not import business module classes |
| R006 `appNotDependOnOtherApp` | `*.app.*` must not import classes from any other module's `*.app.*` (common-app is allowed) |

These rules complement `monodula:check` (which validates Maven pom.xml dependency declarations) by catching violations **in actual Java code** — for example, a static import or reflection-based reference that pom.xml cannot detect.

## Design Principles

1. **Split-ready by design** — Every module can be independently deployed without code changes
2. **Compile-time boundaries** — Maven dependencies enforce rules at compile time, not just conventions
3. **Classpath isolation** — Not depending on `module-x-app` means endpoints are naturally absent
4. **Module autonomy** — Every module self-registers via `spring.factories`; adding or removing a module requires no central config change
5. **Pragmatic data isolation** — Shared database with table-prefix convention (e.g., `t_module_a_xxx`, `t_module_b_xxx`)
6. **Unified config** — Single Apollo project with namespace-per-module isolation

## Requirements

- Java 17+
- Maven 3.8+
- Spring Boot 3.x

## License

[Apache 2.0](LICENSE)
