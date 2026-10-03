# AI 边界（Boundary）— 详细条款

本文件是 `AGENTS.md` 第 1 节的展开。用于判断「AI 能不能动这个文件 / 能不能这么写」。

---

## 1. 判定表

| 路径 / 模式 | 判定 | 理由 |
|---|---|---|
| `src/generated/**` | 🔴 禁止写入 | 由 `runData` 生成，手改必被覆盖且污染 diff |
| `build/**`, `.gradle/**`, `run/**` | 🔴 禁止写入 | 构建产物 |
| `../chk黑洞修复/**` | 🔴 禁止访问 | 复合构建位于仓库之外，路径随机器而变 |
| `**/*.png` `**/*.bbmodel` `**/*.ogg` `**/*.jar` | 🔴 禁止写入 | 二进制，AI 无法可靠生成 |
| `src/main/templates/**` | 🟡 需确认 | 占位符被 `generateModMetadata` 展开，改错导致 mod 无法加载 |
| `gradle.properties`（版本字段） | 🟡 需确认 | MC/Neo/GTCEu 版本牵动全项目 |
| `build.gradle.kts`（依赖段） | 🟡 需确认 | 影响运行时类路径与 jarJar 打包 |
| `settings.gradle.kts` | 🟡 需确认 | 改动会牵动整个模块图 |
| `src/res/*/META-INF/accesstransformer.cfg` | 🟡 需确认 | 提权，等价于关闭 Java 访问控制 |
| `src/res/*/*.mixins.json`（已有条目） | 🟡 需确认 | 删除/改名会让已打包的 mixin 失效 |
| 已有 Mixin 的注入点 | 🟡 需确认 | 上游版本变动即崩溃，且崩溃栈难读 |
| `src/main/<mod>/**/*.java` | 🟢 允许 | 主战场 |
| `src/res/<mod>/**`（新增资源） | 🟢 允许 | 模型、语言、配方、数据包 |
| 新增 Mixin 类 + 在 json 中**新增**条目 | 🟢 允许 | 但必须写清 `@Inject` 的 target 与失败行为 |
| 测试 / GameTest | 🟢 允许 | 优先补测试 |

---

## 2. 逻辑红线详解

### 2.1 core 不联动 GTCEu

`modules/core/build.gradle.kts` 中 GTCEu 依赖被注释，并写明「gtceu core 不联动，去 modpacks 写联动」。

因此：
- `src/main/core/**` 中 **不得** import `com.gregtechceu.*`。
- `src/main/modpacks/**` 可以，且**必须**通过 `implementation(project(":core"))` 复用 core。
- 同理 Create / FTB Quests / JEI 只在 modpacks。

### 2.2 注册必须走 DeferredRegister / GTRegistrate

反例：
```java
// ❌ 绝对禁止
Registry.register(BuiltInRegistries.BLOCK, id("foo"), new FooBlock(props));
```

正例（对齐 `BlockRegistries` / `MachineRegistries` 现有写法）：
```java
// src/main/core/.../init/BlockRegistries.java
public static final DeferredBlock<FooBlock> FOO =
        REGISTER.registerBlock("foo", FooBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.0F));
```
机器类走 `GTRegistrate`（`MachineRegistries#REGISTRATE`）。

### 2.3 副作用延后

禁止在构造 / static init 里注册、读文件、发网络包。延后到：
```java
modBus.addListener(GregtechUniverseCore::commonSetup);
// commonSetup 中：
event.enqueueWork(ClayCauldronInteractions::bootstrap);
```

### 2.4 禁止改已有注册 ID

`gtu_core:clay_cauldron` 这类字符串一旦发布就与存档、命令、结构方块绑定。
需要"改名"时：新增新 ID → 写迁移/兼容 → 保留旧 ID 至少一个版本。

### 2.5 禁止吞异常

```java
// ❌
try { ... } catch (Exception e) { }
try { ... } catch (Exception e) { e.printStackTrace(); }

// ✅
try { ... } catch (Exception e) {
    LOGGER.error("Failed to initialize bulletjme.", e);
    throw new RuntimeException("Failed to initialize Bullet physics native library", e);
}
```
（对齐 `Physics#init` 的现有风格）

### 2.6 禁止跨端泄漏

客户端类不得被服务端静态引用。
- 数据包 / 网络层用 `DistExecutor` 或 payload 分流。
- 客户端渲染相关放 `client/` 子包，仅在 `Dist.CLIENT` 加载。

### 2.7 native / 平台相关代码

`Physics.java` 的 `NativeBundle.detect()` 属于**平台敏感**代码：
- OS/arch 判定分支、jar 路径字符串、库版本号，改动需人工确认。
- 新增平台必须同时在 `modules/physics/build.gradle.kts` 补 `jarJar` 与 `additionalRuntimeClasspath`。

---

## 3. 越界时的正确行为

AI 发现任务落在红线上时，**不要绕过去**，按以下顺序做：

1. 说明哪条红线被触碰，引用本文件对应条目。
2. 给出**不越界的替代方案**（例如：用配置项代替改 `mods.toml`；用 mixin 新增类代替改已有 AT）。
3. 如果必须越界，输出精确的 diff 供人工审阅，**不写入磁盘**。
4. 人工确认后才执行。
