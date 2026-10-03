# 代码规范 — GregTech-Universe

风格基线：**对齐仓库现有代码**。下述规则中，标注「现有风格」的条目来自 `src/main/` 已有文件，新代码必须一致。

---

## 1. 通用

| 项 | 规则 |
|---|---|
| 缩进 | 4 空格，**禁止 tab** |
| 编码 | UTF-8（`build.gradle.kts` 已设 `options.encoding = "UTF-8"`） |
| 换行 | LF，文件末尾保留一个换行 |
| 行宽 | 建议 120 列 |
| 语言 | Java 21，可用 record / switch pattern / sealed |
| import | 不使用通配符 `import x.*`；不 import 未使用项 |

**注释语言**：中英文皆可，但**同一文件内保持统一**。
现有分布：`core` 多为英文，`physics` 多为中文（见 `PhysicsConfig`）。
注释写「为什么」，不写「做了什么」。

```java
// ✅ 解释动机
// 2. 尝试直接从 classpath 加载（开发环境：原生库 jar 直接在 classpath 上）
// ❌ 复述代码
// 循环遍历 zip 条目
```

---

## 2. 命名

| 类型 | 规范 | 示例 |
|---|---|---|
| 类 / 接口 / 枚举 | `PascalCase` | `WaterDamMachine`、`DamMultiblockPatterns` |
| 方法 | `camelCase` | `levelWaterCauldron`、`createMainPattern` |
| 字段 | `camelCase` | `private boolean bootstrapped;` |
| 常量 | `UPPER_SNAKE_CASE` | `EMPTY`、`STACK_PATTERN`、`SPEC` |
| 注册 ID / 资源路径 | `snake_case` | `"clay_cauldron"`、`"block/machine/water_dam_controller"` |
| Mixin 类 | `Mixin` + 目标类名 | `MixinAbstractContainerMenu` |
| Mixin 包 | 目标包 + `.mixin` | `org.polaris2023.gtu.core.mixin.worldgen` |
| 包 | 全小写 | `org.polaris2023.gtu.modpacks.dam` |

---

## 3. 类结构

### 3.1 工具类 / 常量持有类

```java
public final class DamMultiblockPatterns {
    private DamMultiblockPatterns() {
    }
    // ...
}
```
`final class` + `private` 构造 + `static` 方法。见 `DamMultiblockPatterns`、`ClayCauldronInteractions`、`MachineRegistries`。

### 3.2 有可变状态的类

用静态快照 + `@SubscribeEvent` 刷新（见 `PhysicsConfig#onLoad`），
或用 `Atomic*` / `volatile`。**禁止**在跨线程直接读写裸静态字段。

### 3.3 顺序

1. `package`
2. import（项目现有顺序：三方库 → 项目内 → `java.*` 放最后）
3. 类声明
4. `public static final` 常量
5. 静态字段
6. 构造器
7. `public` 方法
8. `private` 方法

---

## 4. 注册（Register）规范

每个模块一个 `init/` 包，按注册表类型分文件：

```
src/main/core/org/polaris2023/gtu/core/init/
    BlockRegistries.java
    ItemRegistries.java
    BlockEntityRegistries.java
    MenuRegistries.java
    AttachmentRegistries.java
    ClayCauldronFluidRegistries.java
    CreativeTabRegistries.java
    Registrykeys.java
    GLMRegistries.java
```

规则：
1. 类名统一 `XxxRegistries`（历史文件 `Registrykeys` 保持原样，不改名）。
2. 常量声明**紧邻同类项**，新项插到同类末尾，不要追加到文件尾部。
3. 属性用 `ofFullCopy(Blocks.X)` / `ofLegacyCopy(Blocks.X)`，不要手写全套 `Properties`。
4. 每个注册类暴露 `public static void register(IEventBus bus)`，在模块入口类调用。
5. GTCEu 机器走 `GTRegistrate`（见 `MachineRegistries`），用 builder 链，禁止绕过。

---

## 5. ResourceLocation

统一用模块入口类的静态方法，**禁止** `new ResourceLocation(...)`：

```java
GregtechUniverseCore.id("path")        // gtu_core:path
GregtechUniverseCore.cid("path")       // c:path        (Create)
GregtechUniverseCore.mid("path")       // minecraft:path
GregtechUniverseModPacks.id("path")    // gtu_modpacks:path
```

---

## 6. Block / Item / Machine

- 自定义 Block 必须实现 `codec()`，缓存 `public static final MapCodec<X> CODEC = simpleCodec(X::new)`。见 `GravelOreBlock`。
- 有状态方块显式声明 `BlockBehaviour.Properties` 中的 `strength` / `sound` / `requiresCorrectToolForDrops`。
- GTCEu 多方块：结构定义集中在 `dam/` 或对应领域包（如 `DamMultiblockPatterns`），**不要**写在机器类里。
- 客户端 tooltip 用 `Component.literal(...)`（现有风格），不硬编码中文到 `lang` 之外的地方。

---

## 7. 网络

- payload 与网络注册集中在 `XxxNetwork`（见 `ModpacksNetwork`），模块入口类只调 `registerPayloads`。
- payload 版本串集中一处（现为 `registrar("1")`），改动需人工确认。
- 处理逻辑必须 `context.enqueueWork(...)` 回到主线程。

---

## 8. 配置

- 用 `ModConfigSpec.Builder`（见 `PhysicsConfig`），带 `.comment()` 与 `defineInRange` 上下界。
- 提供 `getXxx()` 静态访问器，读取缓存字段，不在游戏逻辑里反复 `.get()`。
- 每个维度/世界一组常量，保持字段顺序一致（define → 缓存字段 → onLoad 赋值 → getter）。

---

## 9. Mixin

```java
@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {
    @Inject(method = "lambda$stillValid$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(...)Lnet/minecraft/world/level/block/state/BlockState;"),
            cancellable = true)
    private static void valid(...) { ... }
}
```
- 包路径：`org.polaris2023.gtu.<mod>.mixin`。
- 必须在 `src/res/<mod>/<modId>.mixins.json` 登记；client-only 的另建 `<modId>.client.mixins.json`。
- `defaultRequire: 1`、`compatibilityLevel: JAVA_21`。
- `target` 字符串必须完整精确；新增 mixin 时在注释里写明注入原因。
- **不改已有 mixin 的注入点**（见 `docs/AI-BOUNDARY.md` §1.2）。

---

## 10. 日志

```java
private static final Logger LOGGER = LoggerFactory.getLogger("physics.init");
```
- SLF4J，不用 `System.out`。
- 错误日志必须带异常对象：`LOGGER.error("... ", e)`。
- 初始化类日志名用点分层次（`physics.init`）。

---

## 11. 资源文件

| 类型 | 路径 |
|---|---|
| 模型 | `src/res/<mod>/assets/<modId>/models/block/*.json` |
| 方块状态 | `src/res/<mod>/assets/<modId>/blockstates/*.json` |
| 语言 | `src/res/<mod>/assets/<modId>/lang/*.json` |
| 配方 | `src/res/<mod>/data/<ns>/recipe/*.json` |
| loot | `src/res/<mod>/data/<ns>/loot_table/*.json` |

- JSON 一律 4 空格缩进（与仓库现有资源一致）。
- 生成类资源跑 `gradlew :<mod>:runData`，输出到 `src/generated/<mod>`，**不手写**。

---

## 12. 反模式速查

| 反模式 | 正确做法 |
|---|---|
| `new ResourceLocation("ns","path")` | `Xxx.id("path")` |
| `Registry.register(...)` 直接赋值 | `DeferredRegister` / `GTRegistrate` |
| 构造器里做注册/IO | `FMLCommonSetupEvent` + `enqueueWork` |
| 空 `catch` / `printStackTrace()` | SLF4J 日志 + 明确处理 |
| `gtu_core` 里 import `com.gregtechceu.*` | 联动写进 `gtu_modpacks` |
| 手写 `BlockBehaviour.Properties()` 全套 | `ofFullCopy` / `ofLegacyCopy` |
| 手改 `src/generated/**` | `runData` |
| 一个提交改 5 件事 | 拆分提交 |
| 顺手重排/重命名无关代码 | 保持 diff 最小 |
