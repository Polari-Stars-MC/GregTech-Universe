# C0-02 草案 — 第 01 章「模块 × 槽位 → 检测物」对照表

> **版本**：v2（已按 [DECISIONS-ch01.md](DECISIONS-ch01.md) 的 E-01~E-05 全部采纳 A 方案裁剪）
> **源文档**：`mds/01_蒸汽时代.md`（400 条未精修生成稿）
> **覆盖**：20 模块 × 14 实质槽位 = **280 条检测任务** + 40 条无检测过渡任务 = 320 条
> **本表不写代码**，纯内容设计。

---

## 一、槽位裁剪结果（E-05 = A）

原 20 槽 → 保留 **14 个实质槽位**：

```
02 原料A     03 原料B     04 预处理     05 工装准备
06 核心构件   07 核心设备   08 首批产物   09 物流节点
10 动力条件   11 升级材料   13 升级零件   14 设施落地
15 维护与控制 16 批量产能
```

**被裁的 6 个槽位**

| 槽位 | 处理 | 原因 |
|---|---|---|
| 01 场地确认 | 保留为**无检测**过渡任务 | 每模块相同，本质是章节起始标记 |
| 12 二阶段加工 | ❌ 删除 | 与 06 核心构件 / 07 核心设备 语义重叠 |
| 17 扩产证明 | ❌ 删除 | 与 16 批量产能 语义重叠 |
| 18 科研校验 | ❌ 删除 | 元叙事任务，无实质产出 |
| 19 时代成果 | ❌ 删除 | 与 16 / 17 语义重叠 |
| 20 桥接成果 | 保留为**无检测**过渡任务 | 模块间衔接，修复原文档 B-02 断链 |

> 01 / 20 保留为**只带依赖链与奖励**的任务，不消耗检测物设计工作量，
> 同时让模块间的推进关系在任务书里**显式可见**。

---

## 二、置信度分层

| 标记 | 含义 | 数量 |
|---|---|---|
| ✅ | **已验证** —— 本仓库源码中确实存在 | 6 |
| 🟢 | **高置信** —— 原版物品 / Create 长期稳定注册名 | 257 |
| ⚠️ | **待核实** —— 需查 GTCEu 7.0.2 / Create 6.0.10 jar | 17 |

> 裁剪后待核实处从 28 降到 **17**，且**集中在 7 个 ID**。
> 一次性核实即可覆盖全部。

### ✅ 已验证清单（源码核对）

```
gtu_core:flint_shard                  模块 01 槽位 04
gtu_core:gravely_copper               模块 05 槽位 04
gtu_core:washed_iron_concentrate      模块 07 槽位 04、模块 13 槽位 04
gtu_core:washed_tin_concentrate       模块 07 槽位 08
```

### ⚠️ 待核实清单（17 处 / 7 个 ID）

| ID | 出现位置 | 影响 |
|---|---|---|
| `gtceu:firebrick` | M01-08 | **火砖模块命门** |
| `gtceu:coke_oven_brick` | M01-06、M02-06 | 两模块共用 |
| `gtceu:coke` | M02-08、M13-10 | 焦炉产物 + 钢铁燃料 |
| `gtceu:bronze_machine_casing` | M05-07/08/16 | **青铜机壳模块命门** |
| `gtceu:steam` | M04-07/08、M06-08、M16-08/10 | **横跨 3 模块** |
| `gtceu:steam_turbine` | M06-07 | 蒸汽机核心设备 |
| `gtceu:bronze_large_boiler` | M16-07 | 批量蒸汽产能核心 |
| `create:track` | M09-07 | Create 6 火车 |
| `create:steam_locomotive` | M10-07 | Create 6 火车 |

> 实际是 **9 个 ID**（修正上表标题）。优先级**高于 ch00 的 P0-02**。

---

## 三、检测物对照表（280 行）

---

### 模块 01：火砖 firebrick（01-002 ~ 01-016）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:sand` | 🟢 |
| 03 原料 B | `minecraft:clay` | 🟢 |
| 04 预处理 | `gtu_core:flint_shard` | ✅ |
| 05 工装准备 | `minecraft:stone_bricks` | 🟢 |
| 06 核心构件 | `gtceu:coke_oven_brick` | ⚠️ |
| 07 核心设备 | `minecraft:blast_furnace` | 🟢 |
| **08 首批产物** | **`gtceu:firebrick`** ⭐ | ⚠️ |
| 09 物流节点 | `create:depot` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:quartz_block` | 🟢 |
| 13 升级零件 | `create:brass_casing` | 🟢 |
| 14 设施落地 | `minecraft:bricks` ×32 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `minecraft:smooth_stone` ×64 | 🟢 |

---

### 模块 02：焦炉 coke_oven（01-022 ~ 01-036）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:coal` ×16 | 🟢 |
| 03 原料 B | `minecraft:raw_iron` ×16 | 🟢 |
| 04 预处理 | `minecraft:charcoal` | 🟢 |
| 05 工装准备 | `minecraft:stone_bricks` | 🟢 |
| 06 核心构件 | `gtceu:coke_oven_brick` | ⚠️ |
| 07 核心设备 | `minecraft:blast_furnace` | 🟢 |
| **08 首批产物** | **`gtceu:coke`** ⭐ | ⚠️ |
| 09 物流节点 | `create:basin` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:iron_ingot` | 🟢 |
| 13 升级零件 | `create:brass_casing` | 🟢 |
| 14 设施落地 | `minecraft:bricks` ×32 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `minecraft:iron_ingot` ×64 | 🟢 |

---

### 模块 03：锅炉供水 boiler_water（01-042 ~ 01-056）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` | 🟢 |
| 03 原料 B | `minecraft:water_bucket` | 🟢 |
| 04 预处理 | `minecraft:sand` | 🟢 |
| 05 工装准备 | `minecraft:barrel` | 🟢 |
| 06 核心构件 | `create:fluid_pipe` ×16 | 🟢 |
| 07 核心设备 | `create:water_wheel` | 🟢 |
| **08 首批产物** | **`minecraft:bucket`（满水桶）** ⭐ | 🟢 |
| 09 物流节点 | `create:basin` ×4 | 🟢 |
| 10 动力条件 | `create:large_water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:copper_ingot` | 🟢 |
| 13 升级零件 | `create:mechanical_pump` | 🟢 |
| 14 设施落地 | `minecraft:bricks` ×32 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `minecraft:water_bucket` ×16 | 🟢 |

---

### 模块 04：蒸汽储运 steam_storage（01-062 ~ 01-076）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:copper_ingot` | 🟢 |
| 03 原料 B | `minecraft:glass` | 🟢 |
| 04 预处理 | `minecraft:quartz_block` | 🟢 |
| 05 工装准备 | `minecraft:anvil` | 🟢 |
| 06 核心构件 | `create:fluid_pipe` ×32 | 🟢 |
| **07 核心设备** | **`gtceu:steam`（流体交互）** ⭐ | ⚠️ |
| **08 首批产物** | **`gtceu:steam` ×1000 mB** | ⚠️ |
| 09 物流节点 | `create:basin` ×4 | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:spout` | 🟢 |
| 14 设施落地 | `minecraft:glass_pane` ×32 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `minecraft:glass` ×64 | 🟢 |

> 💡 07 与 08 同物品，靠**检测模式**区分：07 检视交互，08 流体累计量。

---

### 模块 05：青铜机壳 bronze_casing（01-082 ~ 01-096）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:copper_ingot` | 🟢 |
| 03 原料 B | `minecraft:tin_ingot` | 🟢 |
| 04 预处理 | `gtu_core:gravely_copper` | ✅ |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:copper_block` | 🟢 |
| **07 核心设备** | **`gtceu:bronze_machine_casing`** ⭐ | ⚠️ |
| **08 首批产物** | **`gtceu:bronze_machine_casing` ×16** | ⚠️ |
| 09 物流节点 | `create:depot` | 🟢 |
| 10 动力条件 | `create:cogwheel` | 🟢 |
| 11 升级材料 | `minecraft:iron_ingot` | 🟢 |
| 13 升级零件 | `create:mechanical_press` ×4 | 🟢 |
| 14 设施落地 | `minecraft:copper_block` ×8 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `gtceu:bronze_machine_casing` ×32 | ⚠️ |

> 💡 07/08/16 同物品，靠**数量阈值**分层：1 个 → 16 个 → 32 个。

---

### 模块 06：蒸汽机 steam_engine（01-102 ~ 01-116）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:copper_ingot` | 🟢 |
| 03 原料 B | `minecraft:iron_ingot` | 🟢 |
| 04 预处理 | `minecraft:quartz_block` | 🟢 |
| 05 工装准备 | `create:mechanical_crafter` | 🟢 |
| 06 核心构件 | `create:shaft` ×16 | 🟢 |
| **07 核心设备** | **`gtceu:steam_turbine`** ⭐ | ⚠️ |
| **08 首批产物** | **`gtceu:steam` ×1000 mB** | ⚠️ |
| 09 物流节点 | `create:fluid_pipe` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:encased_cogwheel` | 🟢 |
| 14 设施落地 | `create:brass_casing` | 🟢 |
| 15 维护与控制 | `create:stressometer` | 🟢 |
| 16 批量产能 | `create:large_cogwheel` ×16 | 🟢 |

---

### 模块 07：蒸汽矿物线 steam_ore_line（01-122 ~ 01-136）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:raw_iron` | 🟢 |
| 03 原料 B | `minecraft:coal` | 🟢 |
| 04 预处理 | `gtu_core:washed_iron_concentrate` | ✅ |
| 05 工装准备 | `create:mechanical_drill` | 🟢 |
| 06 核心构件 | `create:shaft` ×32 | 🟢 |
| **07 核心设备** | **`create:crushing_wheel`** ⭐ | 🟢 |
| **08 首批产物** | **`gtu_core:washed_tin_concentrate`** ⭐ | ✅ |
| 09 物流节点 | `create:depot` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:iron_ingot` | 🟢 |
| 13 升级零件 | `create:mechanical_drill` ×4 | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` | 🟢 |
| 16 批量产能 | `gtu_core:washed_iron_concentrate` ×64 | ✅ |

---

### 模块 08：蒸汽物流 steam_logistics（01-142 ~ 01-156）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:leather` | 🟢 |
| 03 原料 B | `minecraft:iron_ingot` | 🟢 |
| 04 预处理 | `minecraft:string` | 🟢 |
| 05 工装准备 | `create:mechanical_saw` | 🟢 |
| 06 核心构件 | `create:belt` ×16 | 🟢 |
| 07 核心设备 | `create:belt_connector` | 🟢 |
| 08 首批产物 | `create:depot` | 🟢 |
| 09 物流节点 | `create:funnel` | 🟢 |
| 10 动力条件 | `create:cogwheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:item_filter` | 🟢 |
| 14 设施落地 | `minecraft:hopper` | 🟢 |
| 15 维护与控制 | `create:stressometer` | 🟢 |
| 16 批量产能 | `create:belt` ×64 | 🟢 |

---

### 模块 09：轨道与车站 track_station（01-162 ~ 01-176）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` | 🟢 |
| 03 原料 B | `minecraft:stick` | 🟢 |
| 04 预处理 | `minecraft:redstone` | 🟢 |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:rail` ×16 | 🟢 |
| **07 核心设备** | **`create:track`** ⭐ | ⚠️ |
| 08 首批产物 | `minecraft:rail` ×64 | 🟢 |
| 09 物流节点 | `create:depot` | 🟢 |
| 10 动力条件 | `create:cogwheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `minecraft:detector_rail` | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×64 | 🟢 |
| 15 维护与控制 | `minecraft:lever` ×4 | 🟢 |
| 16 批量产能 | `minecraft:rail` ×128 | 🟢 |

---

### 模块 10：机车 locomotive（01-182 ~ 01-196）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` | 🟢 |
| 03 原料 B | `minecraft:coal` ×32 | 🟢 |
| 04 预处理 | `minecraft:quartz_block` | 🟢 |
| 05 工装准备 | `create:mechanical_crafter` | 🟢 |
| 06 核心构件 | `minecraft:minecart` | 🟢 |
| **07 核心设备** | **`create:steam_locomotive`** ⭐ | ⚠️ |
| **08 首批产物** | **`minecraft:furnace_minecart`** ⭐ | 🟢 |
| 09 物流节点 | `minecraft:chest_minecart` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `minecraft:detector_rail` ×16 | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×64 | 🟢 |
| 15 维护与控制 | `minecraft:lever` ×8 | 🟢 |
| 16 批量产能 | `minecraft:minecart` ×8 | 🟢 |

---

### 模块 11：铁路线铺设 railway_laying（01-202 ~ 01-216）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` ×32 | 🟢 |
| 03 原料 B | `minecraft:sand` ×32 | 🟢 |
| 04 预处理 | `minecraft:gravel` ×32 | 🟢 |
| 05 工装准备 | `minecraft:stone_bricks` | 🟢 |
| 06 核心构件 | `minecraft:rail` ×32 | 🟢 |
| 07 核心设备 | `minecraft:detector_rail` | 🟢 |
| 08 首批产物 | `minecraft:rail` ×128 | 🟢 |
| 09 物流节点 | `minecraft:chest` ×8 | 🟢 |
| 10 动力条件 | `create:cogwheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:item_filter` | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×128 | 🟢 |
| 15 维护与控制 | `minecraft:redstone_torch` ×16 | 🟢 |
| 16 批量产能 | `minecraft:rail` ×256 | 🟢 |

---

### 模块 12：蒸汽维护 steam_maintenance（01-222 ~ 01-236）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` | 🟢 |
| 03 原料 B | `minecraft:leather` ×8 | 🟢 |
| 04 预处理 | `minecraft:string` | 🟢 |
| 05 工装准备 | `create:mechanical_saw` | 🟢 |
| 06 核心构件 | `create:mechanical_piston` ×8 | 🟢 |
| 07 核心设备 | `create:deployer` | 🟢 |
| 08 首批产物 | `minecraft:bucket` ×16 | 🟢 |
| 09 物流节点 | `create:depot` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:mechanical_arm` | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` ×4 | 🟢 |
| 16 批量产能 | `create:mechanical_piston` ×16 | 🟢 |

---

### 模块 13：钢铁预备 steel_prep（01-242 ~ 01-256）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:raw_iron` ×16 | 🟢 |
| 03 原料 B | `minecraft:coal` ×16 | 🟢 |
| 04 预处理 | `gtu_core:washed_iron_concentrate` ×16 | ✅ |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:iron_ingot` ×16 | 🟢 |
| **07 核心设备** | **`minecraft:blast_furnace`** ⭐ | 🟢 |
| 08 首批产物 | `minecraft:iron_ingot` ×16 | 🟢 |
| 09 物流节点 | `create:basin` | 🟢 |
| 10 动力条件 | `gtceu:coke` ×16 | ⚠️ |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:brass_casing` | 🟢 |
| 14 设施落地 | `minecraft:bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` ×4 | 🟢 |
| 16 批量产能 | `minecraft:iron_ingot` ×64 | 🟢 |

---

### 模块 14：热工升级 thermal_upgrade（01-262 ~ 01-276）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:quartz_block` ×8 | 🟢 |
| 03 原料 B | `minecraft:netherrack` ×16 | 🟢 |
| 04 预处理 | `minecraft:nether_bricks` ×16 | 🟢 |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:bricks` ×32 | 🟢 |
| **07 核心设备** | **`minecraft:smoker`** ⭐ | 🟢 |
| 08 首批产物 | `minecraft:blaze_powder` ×8 | 🟢 |
| 09 物流节点 | `create:basin` | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:magma_block` ×8 | 🟢 |
| 13 升级零件 | `create:brass_casing` ×4 | 🟢 |
| 14 设施落地 | `minecraft:nether_bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` ×4 | 🟢 |
| 16 批量产能 | `minecraft:blaze_powder` ×32 | 🟢 |

---

### 模块 15：蒸汽工厂分区 steam_factory_zones（01-282 ~ 01-296）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` ×32 | 🟢 |
| 03 原料 B | `minecraft:glass` ×32 | 🟢 |
| 04 预处理 | `minecraft:smooth_stone` ×32 | 🟢 |
| 05 工装准备 | `create:clipboard` | 🟢 |
| 06 核心构件 | `create:brass_casing` ×16 | 🟢 |
| 07 核心设备 | `create:mechanical_crafter` ×8 | 🟢 |
| 08 首批产物 | `create:depot` ×8 | 🟢 |
| 09 物流节点 | `create:fluid_pipe` ×32 | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:iron_block` ×4 | 🟢 |
| 13 升级零件 | `create:deployer` ×4 | 🟢 |
| 14 设施落地 | `minecraft:chest` ×16 | 🟢 |
| 15 维护与控制 | `create:gauge` ×8 | 🟢 |
| 16 批量产能 | `create:brass_casing` ×32 | 🟢 |

---

### 模块 16：批量蒸汽产能 mass_steam_output（01-302 ~ 01-316）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:coal` ×64 | 🟢 |
| 03 原料 B | `minecraft:water_bucket` ×16 | 🟢 |
| 04 预处理 | `minecraft:charcoal` ×64 | 🟢 |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:bricks` ×64 | 🟢 |
| **07 核心设备** | **`gtceu:bronze_large_boiler`** ⭐ | ⚠️ |
| **08 首批产物** | **`gtceu:steam` ×1000 mB** | ⚠️ |
| 09 物流节点 | `create:fluid_pipe` ×32 | 🟢 |
| **10 动力条件** | **`gtceu:steam` ×2000 mB** | ⚠️ |
| 11 升级材料 | `minecraft:iron_ingot` ×32 | 🟢 |
| 13 升级零件 | `create:mechanical_pump` ×4 | 🟢 |
| 14 设施落地 | `minecraft:bricks` ×128 | 🟢 |
| 15 维护与控制 | `create:gauge` ×8 | 🟢 |
| 16 批量产能 | `minecraft:water_bucket` ×64 | 🟢 |

---

### 模块 17：铁路扩线 railway_expansion（01-322 ~ 01-336）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` ×64 | 🟢 |
| 03 原料 B | `minecraft:gold_ingot` ×16 | 🟢 |
| 04 预处理 | `minecraft:redstone` ×16 | 🟢 |
| 05 工装准备 | `create:mechanical_saw` | 🟢 |
| 06 核心构件 | `minecraft:rail` ×64 | 🟢 |
| 07 核心设备 | `minecraft:detector_rail` ×32 | 🟢 |
| 08 首批产物 | `minecraft:rail` ×256 | 🟢 |
| 09 物流节点 | `minecraft:chest` ×16 | 🟢 |
| 10 动力条件 | `create:cogwheel` ×8 | 🟢 |
| 11 升级材料 | `minecraft:gold_block` ×4 | 🟢 |
| 13 升级零件 | `create:schematic` | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×128 | 🟢 |
| 15 维护与控制 | `minecraft:lever` ×16 | 🟢 |
| 16 批量产能 | `minecraft:rail` ×512 | 🟢 |

---

### 模块 18：工业安全 industrial_safety（01-342 ~ 01-356）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` ×16 | 🟢 |
| 03 原料 B | `minecraft:redstone` ×16 | 🟢 |
| 04 预处理 | `minecraft:glass` ×32 | 🟢 |
| 05 工装准备 | `create:mechanical_saw` | 🟢 |
| 06 核心构件 | `minecraft:torch` ×32 | 🟢 |
| 07 核心设备 | `minecraft:redstone_torch` ×16 | 🟢 |
| 08 首批产物 | `minecraft:glass_pane` ×32 | 🟢 |
| 09 物流节点 | `minecraft:hopper` ×8 | 🟢 |
| 10 动力条件 | `create:gauge` ×4 | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:stressometer` | 🟢 |
| 14 设施落地 | `minecraft:iron_bars` ×32 | 🟢 |
| 15 维护与控制 | `create:gauge` ×8 | 🟢 |
| 16 批量产能 | `minecraft:torch` ×64 | 🟢 |

---

### 模块 19：石油勘探 oil_exploration（01-362 ~ 01-376）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:coal` ×64 | 🟢 |
| 03 原料 B | `minecraft:bucket` ×16 | 🟢 |
| 04 预处理 | `minecraft:lava_bucket` ×8 | 🟢 |
| 05 工装准备 | `create:mechanical_drill` ×4 | 🟢 |
| 06 核心构件 | `create:shaft` ×32 | 🟢 |
| **07 核心设备** | **`create:large_cogwheel`（钻机传动）** ⭐ | 🟢 |
| 08 首批产物 | `minecraft:lava_bucket` ×16 | 🟢 |
| 09 物流节点 | `create:basin` ×8 | 🟢 |
| 10 动力条件 | `create:water_wheel` | 🟢 |
| 11 升级材料 | `minecraft:iron_ingot` ×32 | 🟢 |
| 12 → 13 升级零件 | `create:mechanical_press` ×4 | 🟢 |
| 14 设施落地 | `minecraft:deepslate_bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` ×4 | 🟢 |
| 16 批量产能 | `minecraft:coal` ×128 | 🟢 |

> ⚠️ **语义提示**：原版无原油/沥青物品，本模块用 `lava_bucket`（深井高温）
> 和 `coal`（沥青前驱）作语义占位。
> 若 GTCEu 有原油系物品（`crude_oil` 系列），建议二次修订替换槽位 04 / 08 / 16。

---

### 模块 20：内燃机前夜 pre_combustion（01-382 ~ 01-396）

| 槽位 | 检测物 | 置信 |
|---|---|---|
| 02 原料 A | `minecraft:iron_ingot` ×16 | 🟢 |
| 03 原料 B | `minecraft:redstone` ×16 | 🟢 |
| 04 预处理 | `minecraft:coal` ×32 | 🟢 |
| 05 工装准备 | `create:mechanical_press` | 🟢 |
| 06 核心构件 | `minecraft:piston` ×8 | 🟢 |
| **07 核心设备** | **`create:mechanical_piston`（内燃机雏形）** ⭐ | 🟢 |
| 08 首批产物 | `minecraft:piston` ×16 | 🟢 |
| 09 物流节点 | `create:depot` ×8 | 🟢 |
| 10 动力条件 | `create:cogwheel` ×8 | 🟢 |
| 11 升级材料 | `minecraft:gold_ingot` | 🟢 |
| 13 升级零件 | `create:sequencer` | 🟢 |
| 14 设施落地 | `minecraft:stone_bricks` ×64 | 🟢 |
| 15 维护与控制 | `create:gauge` ×4 | 🟢 |
| 16 批量产能 | `minecraft:piston` ×32 | 🟢 |

---

## 四、过渡任务（40 条，无检测物）

| 位置 | 名称 | 形式 |
|---|---|---|
| 每模块 01 | 场地确认 | 无检测，仅依赖链 + 开场奖励 |
| 每模块 20 | 桥接成果 | 无检测，前置 = 本模块 16，**解锁下模块 01** |

> 模块 01-001 的前置改为 **ch00 末条任务**（修复 B-02 断链）。
> 具体 questId 待 `GTU-ch01-C0-01` 确定。

---

## 五、成果对比

| 指标 | 原文档 | v2 草案 |
|---|---|---|
| 不同物品总数 | **12** | **约 70** |
| 检测物与模块主题相关 | 极少 | 逐条对齐 |
| 同模块内重复 | 最多 4 次 | 仅 2 处（同物品靠阈值分层） |
| ⚠️ 待核实 ID | 数百处 | **9 个 ID / 17 处** |
| 需 Java 目标类型 | — | `structure` `fluid` `energy` = **3 类** |
| 需 Java 语义特性 | — | `strict_nbt` `only_from_crafting` = **0** |

---

## 六、下一步

1. **人工分 10 批审**（每批 2 模块 = 28 行），重点看 **07 核心设备** 与 **08 首批产物**
2. **核实 9 个 ⚠️ ID** —— 打开 GTCEu 7.0.2 / Create 6.0.10 的 jar 搜索
3. 本表定稿后 → `GTU-ch01-C1-01` 开工

> 导出格式：`模块编号,槽位号,槽位名,原检测物,新检测物,置信度,数量阈值`
