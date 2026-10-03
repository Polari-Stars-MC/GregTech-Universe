# 任务规划 — 第 00 章「机械纪元」

> **来源**：`mds/00_机械纪元.md`（75 条任务，11 模块，游戏内 0–40 小时；**实际实现 73 条**）
> **边界依据**：[`AI-BOUNDARY.md`](AI-BOUNDARY.md)　**流程**：[`WORKFLOW.md`](WORKFLOW.md)　**规范**：[`CODE-STYLE.md`](CODE-STYLE.md)
> **决策记录**：[`DECISIONS-ch00.md`](DECISIONS-ch00.md)
> **目标**：把玩家任务拆成 AI 可逐条执行、可验证、带边界档位的开发任务。

---

## 一、代码现状核对（已逐文件验证）

### 已存在，可直接复用

| 内容 | 位置 | 说明 |
|---|---|---|
| `gtu_core:plant_fiber` | `ItemRegistries.PLANT_FIBER` | ✅ 已注册 |
| `gtu_core:flint_shard` | `ItemRegistries.FLINT_SHARD` | ✅ 已注册 |
| `gtu_core:stone_shard` | `ItemRegistries.STONE_SHARD` | ✅ **已注册**（`mds` 标「需新增」，实际已有） |
| `gtu_core:rope` | `ItemRegistries.ROPE` | ✅ **已注册**（`mds` 标「需新增」，实际已有） |
| `gtu_core:flint_crafting_table` | `BlockRegistries.FLINT_CRAFTING_TABLE` | ✅ 方块+物品已注册 |
| `gtu_core:gravely_iron/copper/tin` | `ItemRegistries.GRAVELY_*` | ✅ 已注册（00-057 矿粉可直接用） |
| 砂砾→燧石碎片 | `loot/GravelFlintModifier` | ✅ 20% 概率 |
| 草→植物纤维 | `loot/GrassFiberModifier` | ✅ 70%，**需工具带 `gtu_core:grass_fiber` tag** |
| 工具标签 | `tag/ItemTags.GRASS_FIBER` | ⚠️ tag 已定义，**当前为空** |
| 燧石工作台可用 | `mixin/MixinCraftingMenu` + `MixinAbstractContainerMenu` | ✅ 已解决容器校验 |
| 阶段加成 | `AttachmentRegistries.STAGE_LEVEL` + `ItemTags.STAGE` | ✅ 已有机制 |
| **结构模板框架** | `Registrykeys.DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY`<br>`StructureTemplateDefinition` / `StructureMemberRef`<br>`AttachmentRegistries.STRUCTURE_MEMBER` / `PLACE` | ⭐ 见下 |

> ⭐ **关键发现**：core 已有「结构模板 + 结构成员检测」框架。
> 5 条 `structure` 检测任务（00-049/058/063/064/067）**可能无需从零实现** → 见 P0-03。

### 确认缺失（需开发）

- 工具类物品：`flint_knife` `flint_axe` `flint_pickaxe` `flint_shovel` `stone_*`×4 `bound_flint_*`×4
- 行为：石片获取、燧石右键硬方块、树叶掉木棍
- 配方：绳子、燧石刀、绳绑工具、石片工具、燧石工作台等
- 结构：炭窑（Create/原版均无）
- 数据：全部 73 条任务书
- 自定义目标类型：**7 条**（5×`structure` + 2×`energy`）

---

## 二、前置决策（P0）— ✅ 已全部拍板

> 完整论证、选项对比、修正清单见 [`DECISIONS-ch00.md`](DECISIONS-ch00.md)。

| 编号 | 决策 | 档位 | 状态 | 结论 |
|---|---|---|---|---|
| D-01 | 工具命名空间 | 🔴 | ✅ | **A** — 全部统一 `gtu_core:`，core 不引入 GTCEu |
| D-02 | 大/小齿轮颠倒 | 🔴 | ✅ | **A** — 互换标题，ID 不动 |
| D-03 | FTB Quests 依赖形态 | 🟡 | ✅ | **A** — 改 `implementation`，quest 代码放 modpacks |
| D-04 | 自定义任务目标类型 | 🟡 | ✅ | **A** — 逐条降级，仅 **7 条**需 Java |
| D-05 | 砖模 / 安山合金机制 | 🟡 | ✅ | **A** — Create 原版 Blasting，**删除 00-036/037** |

### 源码证据（供回溯）

| 编号 | 证据 |
|---|---|
| D-01 | `modules/core/build.gradle.kts`：`// gtceu core不联动，去modpacks写联动` |
| D-02 | Create `cogwheel`=小齿轮 / `large_cogwheel`=大齿轮；`mds` 任务表与自身 mermaid 图矛盾 |
| D-03 | `modules/modpacks/build.gradle.kts` 用 `localRuntime(...)`；`build.gradle.kts` 定义其为独立 configuration，编译期不可见 |
| D-04 | `mds` 75 条中非 FTB 原生目标共 14 条 |
| D-05 | Create 安山合金 = Blasting（安山岩+铁锭），与文档「砖模加工」矛盾 |

### 决策带来的变更

| 变更 | 内容 |
|---|---|
| 任务总数 | 75 → **73**（00-036/037 删除，**编号保留空缺不重排**） |
| 工具归属 | 全部在 `gtu_core`；`mds` 中 4 处 `gtceu:*` 改为 `gtu_core:*` |
| 需自研 Java 目标 | 14 条 → **7 条** |
| 安山合金 | Create 原版 Blasting（安山岩+铁锭），零代码 |
| 待回写 `mds/` | M-01 ~ M-06，见 `DECISIONS-ch00.md` |

---

## 三、周期规划

> 每个周期 = 一个可独立验证的垂直切片（内容 + 对应任务书数据）。
> 状态图例：⬜ 未开始　🔄 进行中　✅ 完成　❌ 阻塞

### 周期总览

| 周期 | 名称 | 任务数 | 关联任务编号 | 出口标准 | 状态 |
|---|---|---|---|---|---|
| **P0** | 决策与核实 | 4 | — | 决策落定 + 核实完成 | 🔄 P0-03 ✅ / P0-02 ❌ |
| **P1** | 资源层 | 4 | 00-002~005, 031, 032, 039, 040, 065, 069 | 可产出全部基础资源 | ⬜ |
| **P2** | 工具层 | 10 | 00-006~030 | 三级工具链可制作可使用 | ⬜ |
| **P3** | 材料层 | 3 | 00-033~035, 038, 041, 042, 044 | 砖 + 安山合金 + 机壳 | ⬜ |
| **P4** | 传动与动力 | 5 | 00-043, 045~052, 059~062 | 有应力，水车/风车可转 | ⬜ |
| **P5** | 石磨与物流 | 4 | 00-053~058, 063, 064 | 自动化矿物处理线 | ⬜ |
| **P6** | 炭窑与焦炉 | 4 | 00-066~068, 070~075 | 可产焦炭，章节收口 | ⬜ |
| **P7** | 总装与验收 | 2 | 实际实现 73 条 | 全链路可玩通 | ⬜ |

---

### P0 — 决策与核实 🔄

| ID | 内容 | 档位 | 状态 | 产出 |
|---|---|---|---|---|
| `GTU-ch00-P0-01` | 汇总 D-01~D-05 冲突清单 | 🟢 | ✅ | [`DECISIONS-ch00.md`](DECISIONS-ch00.md) |
| `GTU-ch00-P0-04` | 人工拍板 D-01~D-05 | 🔴 人工 | ✅ | 全部采纳推荐方案 |
| `GTU-ch00-P0-03` | 评估 `structure_template_dynamic` 覆盖率 | 🟢 | ✅ | **部分可行**，见下 |
| `GTU-ch00-P0-02` | 核实 GTCEu/Create 已有物品 | 🟢 | ❌ **未完成** | 需命令工具，见下 |

#### P0-03 结论：🟡 数据模型可复用，但**没有匹配引擎**

已逐文件核对：

| 组件 | 内容 | 状态 |
|---|---|---|
| `StructureTemplateDefinition` | `record(List<StructureNodeDefinition>)` + Codec | ✅ 数据 |
| `StructureNodeDefinition` | `(long relativePos, int expectedStateId, byte flags, int priority)` | ✅ 数据 |
| `StructureMemberRef` | `(UUID networkId, StructureMemberType type)` | ✅ 数据 |
| `AttachmentRegistries.STRUCTURE_MEMBER` | `AttachmentType<StructureMemberRef>` | ⚠️ **未声明 `.sync()`** |
| `AttachmentRegistries.PLACE` | `AttachmentType<Boolean>` | ✅ 仅用于 `GravelFlintModifier` 排除玩家放置方块 |
| `Registrykeys.DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY` | datapack 注册表 | ✅ 仅注册 |

**缺失组件**（均需自研）：

1. ❌ **匹配器** —— 无任何代码遍历世界比对 `relativePos` → `expectedStateId`
2. ❌ **登记流程** —— `networkId` 从哪生成？谁写入 `STRUCTURE_MEMBER` attachment？
3. ❌ **BlockState ID 映射** —— `expectedStateId` 是裸 `int`，需 ID→BlockState 表与存档兼容
4. ❌ **事件钩子** —— 方块放置/破坏时无更新入口
5. ❌ **存档同步** —— `STRUCTURE_MEMBER` 未 `.sync()`

> ⚠️ **修正先前判断**：之前推测「5 条 structure 检测可能无需从零实现」。
> 核实后：数据模型设计方向正确，**可作为自研底座**（省去设计阶段），
> 但匹配器 / 登记 / ID 映射三块仍要写。**5 条 structure 检测合计 3–5 天**，而非零成本。

#### P0-02 状态：❌ 未完成

需检索 GTCEu 7.0.2 / Create 6.0.10 的 jar 内容，当前会话**无命令工具**（无 bash / glob / grep）。
需要核实：

| 物品 | 用途 | 影响 |
|---|---|---|
| `gtceu:coke_oven_brick` | 00-070 焦炉砖 | P6 |
| `gtceu:coke` / `gtceu:coke_oven` | 00-073 焦炭 | **P6 工作量 2 天 vs 6 天的分水岭** |
| `create:gauge` | 00-044 应力表 | P3（低风险） |
| `create:windmill_bearing` | 00-052 风车轴承 | P4 |
| `ftbquests:book` | 00-001 任务书 | P1-04 |

> 解阻方式：运行 `/reload` 启用 PowerShell 工具，或在 IDE 里开 GTCEu jar 搜上述 ID。

**出口**：P0-02 完成 → P1 全面解锁。P0-03 已完成，不阻塞 P1。

> ⚠️ **P0-02 直接决定 P6 工作量**：GTCEu 若自带焦炉 → P6 只需写任务数据（~2 天）；若无 → 需自研 GTCEu 多方块（~6 天）。

---

### P1 — 资源层 ⬜

| ID | 需求 | 档位 | 关联任务 | 涉及文件 |
|---|---|---|---|---|
| `GTU-core-P1-01` | 树叶破坏掉落木棍 | 🟢 | 00-005 | `+ loot/LeafStickModifier.java`、`~ init/GLMRegistries.java`、`+ res/core/data/.../loot_modifiers/*.json` |
| `GTU-core-P1-02` | 燧石右键硬方块 → 燧石碎片 | 🟡 | 00-004 | `+ events/` 监听 `PlayerInteractEvent.RightClickBlock` |
| `GTU-core-P1-03` | 植物纤维掉落链路打通 | 🟢 | 00-009 | `~ tag/ItemTags.java`、tag json（P2-07 回填） |
| `GTU-core-P1-04` | 资源类任务书数据 | 🟡 | 00-002,003,004,005,031,032,039,040,065,069 | `+ res/modpacks/ftbquests/chapters/00_*.snbt` |

**实现要点**
- `P1-02`：硬度 ≥1.5 的判定方式待定 —— NeoForge 未直接暴露方块硬度。
  备选：① 维护白名单 Tag；② 与原版硬度表比对。**已并入 D-01 同批决策，实现前需确认。**
- 参照 `GravelFlintModifier` 的 `LootModifier` + `MapCodec` 写法。
- `P1-04` 依赖 D-03（FTB Quests 改 `implementation`），该改动需单独提出并确认。

**风险**
- 🔴 `P1-02` 会改变**原版燧石**行为（不再点火/插火把）。
- 🟡 与 `P2-04` 共用右键交互通道，**必须先抽出可复用分发器**。

**验证**
- [ ] `gradlew :core:compileJava`
- [ ] `gradlew :core:runData`
- [ ] 人工：挖砂砾出燧石碎片 / 砍树叶出木棍 / 挖黏土 / 挖安山岩 / 挖煤

---

### P2 — 工具层 ⬜

| ID | 需求 | 档位 | 关联任务 |
|---|---|---|---|
| `GTU-core-P2-01` | 燧石刀 `flint_knife` | 🟢 | 00-006 |
| `GTU-core-P2-02` | 绳子（物品已有，补属性/模型/配方） | 🟢 | 00-011 |
| `GTU-core-P2-03` | 燧石斧/镐/锹 | 🟢 | 00-014, 016, 018 |
| `GTU-core-P2-04` | 石片获取：绳绑镐右键石头 | 🟡 | 00-024 |
| `GTU-core-P2-05` | 石片斧/镐/锹/剑 ×4 | 🟢 | 00-026~029 |
| `GTU-core-P2-06` | 绳绑工具 ×4（`bound_flint_*`） | 🟢 | 00-012, 020~022 |
| `GTU-core-P2-07` | 模型 / 语言 / `grass_fiber` tag 回填 | 🟢 | 支撑全部 |
| `GTU-core-P2-08` | 工具属性矩阵设计（耐久/速度/伤害三级） | 🟢 | 支撑 00-013, 023, 030 |
| `GTU-core-P2-09` | 燧石工作台配方（方块已有） | 🟢 | 00-007 |
| `GTU-core-P2-10` | 工具类任务书数据 | 🟡 | 00-006~030 |

**实现要点**
- 工具继承 `DiggerItem` / `SwordItem` 并实现 `codec()`，照抄 `GravelOreBlock` 的 `MapCodec` 写法。
- `P2-04` 与 `P1-02` 共用右键分发器，**先抽分发器再分别接入**。
- `P2-06` 绳绑工具按**新物品**实现（非绑定附魔）。
- `P2-07`：`ItemTags.GRASS_FIBER` 当前为空，`P1-03` 的纤维掉落靠它触发。
- `P2-08` 三级属性递减（燧石 < 绳绑 < 石片）需先定数值表，再写代码。

**风险**
- 🔴 12 个新工具需**纹理 PNG** —— 属 AI 禁区，需人工提供。AI 只写模型 json + 代码。
- 🟡 00-013/023/030 按 D-04 改用 `advancement`，需写 advancement JSON。

**验证**
- [ ] `gradlew :core:compileJava` / `runData`
- [ ] 人工：三级工具链耐久递减符合设计
- [ ] 人工：石片镐挖石头出石片；燧石刀割草出纤维

---

### P3 — 材料层 ⬜

> 已按 D-05 调整：**移除砖模任务**。

| ID | 需求 | 档位 | 关联任务 |
|---|---|---|---|
| `GTU-core-P3-01` | 砖块链路（原版即可，仅任务数据） | 🟢 | 00-033, 034, 035, 038 |
| `GTU-core-P3-02` | 安山合金（Create 原版 Blasting，仅任务数据） | 🟡 | 00-041, 042 |
| `GTU-core-P3-03` | 应力引导 + 应力表任务 | 🟡 | 00-043, 044 |

**实现要点**
- 00-033/034/035 原版已有（黏土块、熔炉烧砖、4砖合1），**零代码**。
- 00-041 安山合金走 Create 原版，**零代码**，但需保证玩家在本章能拿到铁锭：
  原版熔炉烧原矿 + 煤（00-069）或木炭（00-066），**不构成循环依赖**（已验证）。
- 00-038（放置砖块）需确认 FTB 原生是否支持"放置方块"目标。
- 00-044 `create:gauge` 为 Create 自带，仅任务数据。

**验证**
- [ ] 人工：砖链路通；安山合金可制作；安山机壳可制作

---

### P4 — 传动与动力 ⬜

| ID | 需求 | 档位 | 关联任务 |
|---|---|---|---|
| `GTU-ch00-P4-01` | 修正 00-047/048 标题（**出 diff 不落盘**） | 🔴 需确认 | 00-047, 048 |
| `GTU-modpacks-P4-02` | `structure` 检测目标类型（复用 core 框架） | 🟡 | 00-049 |
| `GTU-modpacks-P4-03` | `energy` 检测目标类型（Create 应力 + 适配层） | 🟡 | 00-051, 054 |
| `GTU-modpacks-P4-04` | 传动/动力类任务书数据 | 🟡 | 00-045~052 |
| `GTU-modpacks-P4-05` | 核对全部 `create:*` 物品 ID | 🔴 需确认 | 00-045~052 |

**实现要点**
- Create 只在 `gtu_modpacks` → 所有 `create:*` 引用、应力读取**只能在 modpacks**（红线 §2.1）。
- `energy` 需调 Create 应力 API，属跨模组耦合，**务必封装适配层**，Create 升级才不会全线崩。
- `P4-05` 逐个核对文档中 `create:*` ID 是否真实存在（D-02 已证实文档有错）。

**风险**
- 🔴 Create 6.0.10 API 与网上资料可能不一致 → **必须先读依赖 jar**，不许凭记忆写 API。

**验证**
- [ ] `gradlew :modpacks:compileJava`
- [ ] 人工：水车入水后应力表读数 > 0；齿轮链相邻放置能带动

---

### P5 — 石磨与物流 ⬜

| ID | 需求 | 档位 | 关联任务 |
|---|---|---|---|
| `GTU-modpacks-P5-01` | 石磨接入与矿粉链路 | 🟢 | 00-053~057 |
| `GTU-modpacks-P5-02` | 物流件任务数据 | 🟡 | 00-059~062 |
| `GTU-modpacks-P5-03` | 结构检测任务（石磨线、物流线） | 🟡 | 00-058, 063, 064 |
| `GTU-modpacks-P5-04` | 明确 00-057 检测物为 `gravely_iron/copper/tin` | 🟢 | 00-057 |

**风险**
- 🟡 00-057 原写「矿石粉末」未指定 ID → 已由 P5-04 明确为已注册的 `gravely_*`。

**验证**
- [ ] 人工：水车→传动轴→石磨全线运转；圆石→砂砾→燧石链路成立

---

### P6 — 炭窑与焦炉 ⬜

| ID | 需求 | 档位 | 关联任务 |
|---|---|---|---|
| `GTU-core-P6-01` | 原木→木炭（**先验证原版熔炉是否足够**） | 🟢 | 00-065, 066 |
| `GTU-core-P6-02` | 炭窑结构（**设计决策 🟡**） | 🟡 | 00-067, 068 |
| `GTU-modpacks-P6-03` | 焦炉多方块（**取决于 P0-02 核实结果**） | 🟡 | 00-070~073 |
| `GTU-modpacks-P6-04` | 章节收口 + 蒸汽时代衔接 | 🟡 | 00-074, 075 |

**实现要点**
- `P6-01`：原版熔炉即可，若满足则**零代码**。
- `P6-02` 炭窑：**原版与 Create 均无** → 需自研。优先复用 core 的 `structure_template_dynamic` 框架做动态结构。
- `P6-03` 焦炉：GTCEu 7.x 疑似自带 Coke Oven，若存在则**只写任务数据**。
- `P6-04`：按 D-04，00-074 降级为 `item`，00-075 改为章节终点任务。

**风险**
- 🔴 若 GTCEu 无焦炉 → 需在 modpacks 实现 GTCEu 多方块，工作量与风险显著上升。

**验证**
- [ ] 人工：炭窑可批量产木炭；焦炉可产焦炭

---

### P7 — 总装与验收 ⬜

| ID | 内容 | 档位 |
|---|---|---|
| `GTU-ch00-P7-01` | 73 条任务全链路走通（新存档实机） | 🟡 |
| `GTU-ch00-P7-02` | 回写 `mds/` 的 M-01 ~ M-06 修正 | 🔴 需确认 |

**出口标准**
- [ ] 新建存档，从 00-001 做到 00-075 无断链
- [ ] 全部前置依赖无环、无悬空引用
- [ ] 无任务因物品 ID 不存在而卡死
- [ ] `gradlew :core:compileJava` / `:modpacks:compileJava` / `:physics:compileJava` 全通过

---

## 四、依赖图

```
P0(决策 ✅ + 核实 🔄)
 └─→ P1(资源) ──┐
                ├─→ P2(工具) ──→ P3(材料) ──→ P4(传动) ──→ P5(石磨物流) ──→ P6(炭窑焦炉) ──→ P7(验收)
                │      │             │
                │      └─────────────┴──→ P1-03 tag 回填（依赖 P2-07）
                │
                └─→ P0-02/P0-03 核实结论 ──→ P6-02 / P6-03 / P4-02 设计
```

**硬依赖（已随决策解除）**

| 原依赖 | 状态 |
|---|---|
| P2 ← D-01 | ✅ 已解除（统一 `gtu_core:`） |
| P2 ← D-02 | ✅ 已解除（互换标题） |
| P4+ ← D-03 | ✅ 已解除（改 `implementation`，**实施时仍需单独确认该行变更**） |
| P4/P5/P6 ← D-04 | ✅ 已解除（仅 7 条需 Java） |
| P3 ← D-05 | ✅ 已解除（砖模任务移除） |

**仍然存在的依赖**

- `P1-03` 纤维 tag 回填 **依赖 `P2-07`**（P1 阶段只能完成链路）
- `P1-02` 与 `P2-04` **共用右键交互分发器**，需先抽出再分别接入
- `P6-03` **完全取决于 P0-02** 核实结果
- `P4-02` **完全取决于 P0-03** 评估结果
- 所有任务书数据（`P1-04` 起）**依赖 D-03 实施**

---

## 五、风险登记

| # | 风险 | 状态 | 应对 |
|---|---|---|---|
| R-01 | 工具命名空间与 core 红线冲突 | ✅ 已解决 | D-01 A |
| R-02 | FTB Quests 依赖形态 | 🟡 决策已定，**实施待确认** | D-03 A，改 `build.gradle.kts` 需单独确认 |
| R-03 | 焦炉/炭窑是否需自研 | ⬜ 未知 | P0-02 优先核实 |
| R-04 | Create API 凭记忆写错 | ⬜ 开放 | 强制先读依赖 jar |
| R-05 | 12 个新工具缺纹理（🔴 禁区） | ⬜ 开放 | 人工提供 PNG，AI 只写 json+代码 |
| R-06 | `structure_template_dynamic` 可行性 | ✅ **已核实** | 数据模型可复用，匹配器需自研；structure 检测 3–5 天 |
| R-07 | 文档中 Create ID 多处错误 | ⬜ 开放 | P4-05 逐个核对，先出 diff |
| R-09 | P0-02 依赖 jar 检索未完成 | ⬜ **阻塞** | 需 `/reload` 或人工在 IDE 检索 |
| R-08 | `P1-02` 改变原版燧石行为 | ⬜ 开放 | 需确认是否接受 |

---

## 六、建议排期

| 周期 | 建议长度 | 并行性 | 关键前置 |
|---|---|---|---|
| P0 | 余 0.5 天（仅核实） | 人工密集 | — |
| P1 | 3–4 天 | 可与 P0 尾段并行 | P0-02 |
| P2 | 5–7 天 | 单线（依赖重） | 人工提供纹理 |
| P3 | 1–2 天 | 可与 P2 尾段并行 | — |
| P4 | 4–5 天 | 单线 | D-03 实施 + P0-03 |
| P5 | 3–4 天 | 单线 | P4 |
| P6 | 2–6 天 | 单线 | **P0-02** |
| P7 | 2–3 天 | 单线 | 全部 |

> P6 的 2–6 天区间完全取决于 P0-02 核实结果。

---

## 七、附录：任务编号 → 周期映射

| 章节模块 | 任务数 | 编号 | 所属周期 |
|---|---|---|---|
| 01 燧石与木棍 | 8 | 00-001~008 | P1（001~005）、P2（006~008） |
| 02 植物纤维与绳子 | 5 | 00-009~013 | P1（009）、P2（010~013） |
| 03 燧石工具 | 6 | 00-014~019 | P2 |
| 04 绳绑燧石工具 | 4 | 00-020~023 | P2 |
| 05 石片工具 | 7 | 00-024~030 | P2 |
| 06 黏土与砖 | 6 | 00-031~038 | P1（031,032）、P3（033~035,038）<br>~~036,037 已删除（D-05）~~ |
| 07 安山合金 | 6 | 00-039~044 | P1（039,040）、P3（041~044） |
| 08 传动系统 | 8 | 00-045~052 | P4 |
| 09 石磨工艺 | 6 | 00-053~058 | P5 |
| 10 物流基础 | 6 | 00-059~064 | P4（059~062）、P5（063,064） |
| 11 炭窑与焦炉 | 11 | 00-065~075 | P6（065~068, 070~075）、P7（收口） |
| **合计** | **73**（原 75） | 编号 00-001~00-075 保留空缺 | — |
