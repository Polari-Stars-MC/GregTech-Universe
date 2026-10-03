# ch06~13 模块改写 — diff 规格

> **性质**：**规格，不是可直接 `apply` 的补丁。** AI 无权写 `mds/`。
> **依据**：[`DECISIONS-ch06to13.md`](DECISIONS-ch06to13.md) 的 `H-02`
> **规模**：160 个模块中 **139 个改名**，21 个保留
> **影响面**：约 **19,599 处字符串**（含中文名 + 英文 slug）
> **执行方式**：✅ 已出可执行脚本 [`tools/rename-ch06to13.ps1`](../tools/rename-ch06to13.ps1) + [`tools/rename-map.csv`](../tools/rename-map.csv)
> **状态**：⏳ 等人工跑一条命令（AI 无 shell 权限）

---

## 零、修正：ch04 的 244 处是错的

在实测 ch04 单任务块结构后（`mds/04_MV.md` L30–L92），原先的估算有误。

### 实测：单任务块的字段结构

```
#### 04-002 结构钢 / 资源发现 / 原料 A          ← ① 标题含模块名
- `任务编号`：`04-002`                            ← 无模块名
- `所属阶段`：第 04 章 MV                         ← 无
- `所属子模块`：结构钢                            ← ② 含
- `工业史定位`：第 04 章 MV / 资源发现 / 原料 A    ← ③ ❗不含（装的是子阶段+槽位）
- `任务目标`：取得“结构钢”所需的第一类基础原料…（文档签名：04-002 / 第 04 章 MV / structural_steel）
                                              ← ④ 含中文名 + ⑤ 含 slug
- `前置任务` / `后续影响`                        ← 无
- `检测方式`：item                               ← 无
- `检测对象`：minecraft:iron_block（检测签名：TWQ-04-structural_steel-02）
                                              ← ⑥ 含 slug
- `是否要求严格 NBT` / `only_from_crafting`       ← 无
- `配方/机器/结构关联`：结构钢、minecraft:iron_block、questId 140002
                                              ← ⑦ 含
- `玩家行动说明`：玩家需要主动采集、勘探或开采资源…  ← ⑧ slot 01 才有中文名，其他槽无
- `现实依据` / `推荐奖励` / `平衡性说明`           ← 无
- `实现备注`：建议映射到 questId 140002，并在需要时给检测对象挂上签名 TWQ-04-structural_steel-02。
                                              ← ⑨ 含 slug
```

> 🔴 **`工业史定位` 从不包含模块名。** 我在 `ch04-rename-diff.md` 里把它算成了 9 类字段之一，
> 这是错的。**该文档需要修正。**

### 单模块实际改动数

| 位置 | 中文名 | slug | 小计 |
|---|---|---|---|
| ① 标题 | 20 | 0 | 20 |
| ② `所属子模块` | 20 | 0 | 20 |
| ③ `工业史定位` | **0** | 0 | 0 |
| ④ `任务目标` | 20 | 20 | 40 |
| ⑥ `检测对象` | 0 | 20 | 20 |
| ⑦ `配方/机器/结构关联` | 20 | 0 | 20 |
| ⑧ `玩家行动说明` | 1 ⚠️ | 0 | 1 |
| ⑨ `实现备注` | 0 | 20 | 20 |
| **合计 / 模块** | **81** | **60** | **141** |

⚠️ ⑧ 只有 slot 01（场地确认）含模块名，其余 19 槽为通用文案 —— **此结论来自 ch04 抽样，ch06–13 需 grep 复核**。

**修正后：**

| 项 | 原估 | 实测 |
|---|---|---|
| ch04 模块 15/16 | 244 处 / 9 类字段 | **282 处 / 8 类字段**（141 × 2） |
| ch06–13 单模块 | — | **141 处** |
| ch06–13 全量（138 模块） | — | **约 19,458 处** |

---

## 一、🔴 执行铁律

### 铁律 1：禁止替换裸词 `维护` / `控制` / `扩产`

这三个词**既是模块名的组成部分，又是子阶段名的组成部分**：

```
子阶段：维护与控制     ← 含「维护」「控制」
子阶段：规模化扩产     ← 含「扩产」
模块：  轨道维护 / 基地维护 / 聚变维护 / 深空维护 / 云阵列维护 / 恒星工程维护 / 基地维护 / 可复用维护
模块：  卫星控制 / 等离子控制 / 姿态控制 / 宇航控制 / 远程控制 / 超高阶控制 / 温度控制 / 终局控制
模块：  聚变扩产
```

> **必须替换完整模块名字符串，绝不能替换裸词。**
> 反例：`替换「维护」→「超大规模维护」` 会把 9 处子阶段 `维护与控制` 一起毁掉。

### 铁律 2：slug 替换天然安全

slug 是纯 ASCII（`structural_steel`、`orbital_maintenance`…），与中文子阶段名无冲突，
**slug 替换可以批量做，中文名替换必须逐模块做。**

### 铁律 3：slug 必须先核实

> ⚠️ **本文档中「原 slug」列为 🟢 待核实。**
> 我只实测过 `mds/06_EV.md` 模块 01 的 `TWQ-06-…` 与 `mds/04_MV.md` 的 `structural_steel`。
> ch07–13 的 slug **一个都没实测**。
>
> **应用前必须先用 grep 导出每章的真实 slug**，否则会替换不存在的字符串（静默失败）。

### 铁律 4：检测签名同步

模块改名 ⇒ 20 条检测签名 `TWQ-<章号>-<slug>-<两位槽位>` 全部改变。
这**不是** bug，是设计使然——签名绑定模块身份，模块改名签名就该变。
但**必须同一次提交改完**，否则中途会出现签名断裂。

---

## 二、138 个改名模块对照表

> 槽位编号 `00`–`19` 对应 questId 末两位模块段。
> ✅ = 保留原名，不动。

### 第 06 章 EV —— 电子化革命（18 改名 / 2 保留）

| 槽 | 原模块名 | 新模块名 | 原 slug（⚠️ 待核实） | 新 slug |
|---|---|---|---|---|
| 00 | 晶体管 ✅ | — | `vacuum_tube` ✅实测 | — |
| 01 | 卫星总线 | 集成电路基材 | 🟢 `satellite_bus` | `ic_substrate` |
| 02 | 轨道通信 | 高频电子元件 | 🟢 `orbital_comms` | `rf_electronics` |
| 03 | 载荷 | 高密度电路板 | 🟢 `payload` | `dense_circuit_board` |
| 04 | 轨道发射 | 晶圆制造 | 🟢 `orbital_launch` | `wafer_fabrication` |
| 05 | 返回舱 | 电子封装 | 🟢 `return_capsule` | `electronic_packaging` |
| 06 | 生命支持 | 高纯气体处理 | 🟢 `life_support` | `high_purity_gas` |
| 07 | 卫星控制 | 精密控制电路 | 🟢 `satellite_control` | `precision_control_circuit` |
| 08 | 轨道补给 | 电子元件物流 | 🟢 `orbital_resupply` | `component_logistics` |
| 09 | 姿态控制 | 精密伺服 | 🟢 `attitude_control` | `precision_servo` |
| 10 | 深空通信 | 长距离信号传输 | 🟢 `deep_space_comms` | `long_range_signal` |
| 11 | 电子升级 ✅ | — | — | — |
| 12 | 极高压供电 | EV级储能 | 🟢 `ev_power` | `ev_energy_storage` |
| 13 | 太空地面站 | 数据中心 | 🟢 `space_ground_station` | `data_center` |
| 14 | 载人飞行准备 | 可靠性测试 | 🟢 `crewed_flight_prep` | `reliability_testing` |
| 15 | 轨道证明 | EV电子产线证明 | 🟢 `orbital_proof` | `ev_electronics_line` |
| 16 | 轨道维护 | 电子设备维护 | 🟢 `orbital_maintenance` | `electronics_maintenance` |
| 17 | 太空奇观 | 高性能计算 | 🟢 `space_wonder` | `hpc` |
| 18 | IV前夜 | 高阶电子材料 | 🟢 `iv_eve` | `advanced_electronics` |
| 19 | 月球准备 | 下一代电子材料 | 🟢 `moon_prep` | `next_gen_electronics` |

---

### 第 07 章 IV —— 超大规模电网外送（20 改名）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 集成电路 | 高压绝缘材料 | `hv_insulation` |
| 01 | 多级火箭 | 多级变电站 | `multistage_substation` |
| 02 | 月面着陆 | 特高压并网 | `uhv_grid_connection` |
| 03 | 月壤采样 | 远程地质勘探 | `remote_geological_survey` |
| 04 | 月球基地舱 | 特高压枢纽站 | `uhv_hub_station` |
| 05 | 空间站舱段 | 电网分段 | `grid_segmentation` |
| 06 | 对接 | 并网接口 | `grid_tie_interface` |
| 07 | 长驻支持 | 长距离输电 | `long_distance_transmission` |
| 08 | 轨道冶炼 | 高压电弧冶炼 | `hv_arc_smelting` |
| 09 | 月面物流 | 跨区域输电 | `interregional_transmission` |
| 10 | 低重力制造 | 便携式变电站 | `portable_substation` |
| 11 | 基地维护 | 电网维护 | `grid_maintenance` |
| 12 | 航天服 | 高压作业装备 | `hv_work_gear` |
| 13 | 宇航控制 | 电网调度中心 | `grid_dispatch_center` |
| 14 | 批量发射 | 批量并网 | `batch_grid_connection` |
| 15 | 空间站扩建 | 电网扩建 | `grid_expansion` |
| 16 | 月面产能 | 跨洲电网产能 | `intercontinental_grid_capacity` |
| 17 | 科学载荷 | 高压实验 | `hv_experiment` |
| 18 | 深空跳板 | 特高压前驱 | `uhv_precondition` |
| 19 | LuV前夜 | 更高电压等级 | `higher_voltage_tier` |

---

### 第 08 章 LuV —— 超低品位选矿（20 改名）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 微处理器 | 高精度控制 | `precision_control` |
| 01 | 可重复使用航天器 | 可复用产线平台 | `reusable_line_platform` |
| 02 | 机械臂 | 自动化采矿臂 | `automated_mining_arm` |
| 03 | 轨道总装 | 大型选矿厂总装 | `large_mill_assembly` |
| 04 | 深空探测器 | 深孔勘探 | `deep_bore_survey` |
| 05 | 火星窗口 | 低品位矿脉 | `low_grade_orebody` |
| 06 | 轨道燃料 | 高品位精矿 | `high_grade_concentrate` |
| 07 | 轨道仓储 | 大规模矿料仓储 | `bulk_ore_storage` |
| 08 | 深空导航 | 矿体勘探导航 | `orebody_survey_navigation` |
| 09 | 计算升级 | 选矿控制升级 | `mill_control_upgrade` |
| 10 | 远程控制 | 远程矿山调度 | `remote_mine_dispatch` |
| 11 | 深空载荷 | 采选设备 | `beneficiation_equipment` |
| 12 | 火星前哨准备 | 露天矿前期 | `open_pit_preparation` |
| 13 | 再入回收 | 尾矿回收 | `tailings_recovery` |
| 14 | 可复用维护 | 设备复用维护 | `equipment_reuse_maintenance` |
| 15 | 深空工业 | 规模化采选工业 | `large_scale_beneficiation` |
| 16 | 批量探测 | 批量勘探 | `batch_survey` |
| 17 | 火星证明 | 露天矿产能证明 | `open_pit_capacity_proof` |
| 18 | ZPM前夜 | 超大规模矿业 | `mega_scale_mining` |
| 19 | 行星开发 | 更深地壳开采 | `deeper_crust_mining` |

---

### 第 09 章 ZPM —— 行星际物流与超大规模制造（20 改名）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 火星基地 | 行星级制造基地 | `planetary_manufacturing_base` |
| 01 | 火星冶炼 | 超大电弧炉 | `mega_arc_furnace` |
| 02 | 火星农业 | 大规模化工原料 | `bulk_chemical_feedstock` |
| 03 | 小行星拖船 | 超长距离物流 | `ultra_long_logistics` |
| 04 | 小行星捕获 | 超大储量锁定 | `mega_reserve_acquisition` |
| 05 | 小行星采矿 | 露天矿规模化开采 | `open_pit_mega_mining` |
| 06 | 轨道转运 | 跨区域转运 | `interregional_transfer` |
| 07 | 火星物流 | 跨基地物流 | `interbase_logistics` |
| 08 | 地外燃料 | 合成燃料 | `synthetic_fuel` |
| 09 | 太空自动化 | 全自动工厂 | `fully_automated_factory` |
| 10 | 基地维护 | 基地级维护 | `base_level_maintenance` |
| 11 | 远程调度 | 跨基地调度 | `interbase_dispatch` |
| 12 | 轨道补给站 | 物流枢纽 | `logistics_hub` |
| 13 | 火星扩建 | 基地扩建 | `base_expansion` |
| 14 | 批量矿业 | 批量开采 | `batch_mining` |
| 15 | 行星间运输 | 超大规模运输网 | `mega_transport_network` |
| 16 | 火星科研 | 工艺研发 | `process_development` |
| 17 | 行星证明 | 行星级产能证明 | `planetary_capacity_proof` |
| 18 | UV前夜 | 更高规模 | `higher_scale` |
| 19 | 外太阳系准备 | 跨星系前驱 | `interstellar_precondition` |

---

### 第 10 章 UV —— 极端环境材料（18 改名 / 2 保留）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 外太阳系探测 | 极端环境勘探 | `extreme_env_survey` |
| 01 | 木星转移 | 超远距离物流 | `ultra_long_range_logistics` |
| 02 | 核热推进 | 高能量密度材料 | `high_energy_density_material` |
| 03 | 耐辐射材料 ✅ | — | — |
| 04 | 深低温存储 | 低温材料 | `cryogenic_material` |
| 05 | 冰卫星钻探 | 深部钻探 | `deep_drilling` |
| 06 | 外行星基地 | 极端环境基地 | `extreme_env_base` |
| 07 | 长航时支持 | 长效能源系统 | `long_duration_power_system` |
| 08 | 核热物流 | 高能燃料物流 | `high_energy_fuel_logistics` |
| 09 | 长程通信 | 超远距离通信 | `ultra_long_range_comms` |
| 10 | 极限供电 | 超限电力 | `extreme_power` |
| 11 | 深空维护 | 极端环境维护 | `extreme_env_maintenance` |
| 12 | 极端环境制造 ✅ | — | — |
| 13 | 远征扩建 | 大规模扩建 | `large_scale_expansion` |
| 14 | 批量外行星采集 | 批量采集 | `batch_extraction` |
| 15 | 科学载荷升级 | 高精度仪器 | `precision_instrument` |
| 16 | 冰卫星证明 | 极端采矿证明 | `extreme_mining_proof` |
| 17 | UHV前夜 | 更高能级 | `higher_energy_tier` |
| 18 | 聚变准备 | 聚变材料前驱 | `fusion_material_precondition` |
| 19 | 星际前驱 | 星际材料前驱 | `interstellar_material_precondition` |

---

### 第 11 章 UHV —— 聚变能源（**12** 改名 / **8** 保留 ✅）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 聚变材料 ✅ | — | — |
| 01 | 托卡马克 ✅ | — | — |
| 02 | 聚变点火 ✅ | — | — |
| 03 | 等离子控制 ✅ | — | — |
| 04 | 光帆 | 超大规模集能阵列 | `mega_energy_harvest_array` |
| 05 | 恒星际探测器 | 远程自动化探测站 | `remote_autonomous_station` |
| 06 | 世代飞船前体 | 超大型预制产线 | `mega_prefab_line` |
| 07 | 长程生态支持 | 长程能源支持 | `long_duration_energy_support` |
| 08 | 聚变物流 ✅ | — | — |
| 09 | 聚变维护 ✅ | — | — |
| 10 | 超长程导航 | 跨区域物流调度 | `interregional_logistics_dispatch` |
| 11 | 远程科研 ✅ | — | — |
| 12 | 深空制造 | 超大规模制造 | `mega_scale_manufacturing` |
| 13 | 聚变扩产 ✅ | — | — |
| 14 | 恒星际通信 | 远程监控网络 | `remote_monitoring_network` |
| 15 | 第一艘星际探测器 | 远程设施首期工程 | `remote_facility_phase_one` |
| 16 | 星际证明 | 聚变产能证明 | `fusion_capacity_proof` |
| 17 | UEV前夜 | 更高能级 | `higher_energy_tier` |
| 18 | 戴森前驱 | 超能级能源前驱 | `ultra_energy_precondition` |
| 19 | 恒星采能准备 | 大规模采能准备 | `large_scale_harvest_prep` |

> ✅ **本章 9 / 20 模块零改动**，是 ch06–13 中改写量最小、工期最低的一章。

---

### 第 12 章 UEV —— 超大规模能源采集与集群制造（17 改名 / 3 保留）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 分子制造 | 原子级加工 | `atomic_precision_machining` |
| 01 | 轨道工厂集群 | 多基地制造集群 | `multibase_manufacturing_cluster` |
| 02 | 采能卫星 | 大规模能源采集 | `large_scale_energy_harvest` |
| 03 | 戴森云骨架 | 能源采集网骨架 | `energy_harvest_framework` |
| 04 | 轨道物流阵列 | 超大物流阵列 | `mega_logistics_array` |
| 05 | 恒星能量汇集 | 能源汇集枢纽 | `energy_hub` |
| 06 | 自动货船 | 全自动运输 | `fully_automated_transport` |
| 07 | 行星级调度 | 跨基地调度 | `interbase_dispatch` |
| 08 | 大规模制造 ✅ | — | — |
| 09 | 超高阶控制 ✅ | — | — |
| 10 | 能源缓存 ✅ | — | — |
| 11 | 外行星供能 | 远程供能 | `remote_power_supply` |
| 12 | 戴森扩建 | 采集网扩建 | `harvest_network_expansion` |
| 13 | 批量恒星采能 | 批量能源采集 | `batch_energy_harvest` |
| 14 | 云阵列维护 | 采集网维护 | `harvest_network_maintenance` |
| 15 | 戴森云证明 | 能源采集产能证明 | `energy_harvest_capacity_proof` |
| 16 | UIV前夜 | 更高能级 | `higher_energy_tier` |
| 17 | 恒星工程准备 | 超能级能源准备 | `ultra_energy_prep` |
| 18 | 行星改造准备 | 环境工程前驱 | `environmental_engineering_precondition` |
| 19 | 星际基建 | 超级基地 | `super_base` |

---

### 第 13 章 UIV —— 闭环生态工业与终局材料（14 改名 / 6 保留）

| 槽 | 原模块名 | 新模块名 | 新 slug |
|---|---|---|---|
| 00 | 恒星调光结构 | 大规模能量调控 | `large_scale_energy_regulation` |
| 01 | 行星改造设备 | 大规模环境工程设备 | `large_scale_env_equipment` |
| 02 | 大气处理 ✅ | — | — |
| 03 | 温度控制 ✅ | — | — |
| 04 | 生态圈 | 闭环生态工业 | `closed_loop_ecology_industry` |
| 05 | 火星地球化 | 大规模环境改造 | `large_scale_terraforming` |
| 06 | 金星改造前驱 | 极端环境改造 | `extreme_env_renovation` |
| 07 | 行星级制造 | 超大规模制造 | `mega_scale_manufacturing` |
| 08 | 星际物流稳定 | 超大规模物流 | `mega_scale_logistics` |
| 09 | 恒星工程维护 | 超大规模维护 | `mega_scale_maintenance` |
| 10 | 超大型控制网络 ✅ | — | — |
| 11 | 生态链闭环 | 工业废料闭环 | `industrial_waste_loop` |
| 12 | 批量改造单元 | 批量环境单元 | `batch_environmental_unit` |
| 13 | 行星证明 | 终局产能证明 | `endgame_capacity_proof` |
| 14 | 恒星工程证明 | 终局能源证明 | `endgame_energy_proof` |
| 15 | ⚠️ **UMV前夜** | **终局阶段** | `endgame_phase` |
| 16 | 终局材料 ✅ | — | — |
| 17 | 终局能量 ✅ | — | — |
| 18 | 终局控制 ✅ | — | — |
| 19 | 戴森闭环准备 | 能源闭环准备 | `energy_loop_prep` |

> 🔴 槽位 15 是 `G13-03` 的目标：消除 `UMV` 悬空引用。
> ⚠️ 槽位 13 与槽位 16 同名后缀（`行星证明`→`终局产能证明` vs 原 `终局材料`）不冲突，但需人工确认语义不重复。

---

## 三、改动量汇总

| 章 | 改名 | 保留 | 中文名处 | slug 处 | 合计 |
|---|---|---|---|---|---|
| 06 | 18 | 2 | 1,458 | 1,080 | **2,538** |
| 07 | 20 | 0 | 1,620 | 1,200 | **2,820** |
| 08 | 20 | 0 | 1,620 | 1,200 | **2,820** |
| 09 | 20 | 0 | 1,620 | 1,200 | **2,820** |
| 10 | 18 | 2 | 1,458 | 1,080 | **2,538** |
| 11 | **12** ⚠️ | **8** ⚠️ | 972 | 720 | **1,692** |
| 12 | 17 | 3 | 1,377 | 1,020 | **2,397** |
| 13 | 14 | 6 | 1,134 | 840 | **1,974** |
| **合计** | **139** | **21** | **11,259** | **8,340** | **19,599** |

⚠️ **本表已修正一处计错**：初版记 ch11 = 11 改名 / 9 保留，逐槽核对后实为 **12 / 8**
（槽 04/05/06/07/10/12/14/15/16/17/18/19 改名，共 12 个）。
全包总数由 138 → **139**，处数由 19,458 → **19,599**。

---

## 四、执行方式：脚本已就绪

| 文件 | 作用 |
|---|---|
| [`tools/rename-map.csv`](../tools/rename-map.csv) | 139 行映射（UTF-8） |
| [`tools/rename-ch06to13.ps1`](../tools/rename-ch06to13.ps1) | 执行器（**默认 dry run**） |

```powershell
# 1. 先看会发生什么（不写任何文件）
powershell -ExecutionPolicy Bypass -File tools\rename-ch06to13.ps1

# 2. 确认无误后真正写入
powershell -ExecutionPolicy Bypass -File tools\rename-ch06to13.ps1 -Apply

# 3. 只做一章（试点推荐 ch11）
powershell -ExecutionPolicy Bypass -File tools\rename-ch06to13.ps1 -Apply -Chapter 11
```

### 脚本内置的 4 道守卫

| # | 守卫 | 作用 |
|---|---|---|
| 1 | `维护与控制` 计数不变 | 铁律 1：验证子阶段没被裸词替换毁掉 |
| 2 | `规模化扩产` 计数不变 | 同上 |
| 3 | `任务编号` 计数不变 | 验证任务数没变（应为 400） |
| 4 | `工业史定位` 计数不变 | 验证删后字段结构完整 |

任一不满足 → **该文件拒绝写入**，并打印原因。

### 脚本还会报的东西

- **前缀冲突**（某旧名是另一旧名的前缀）→ 警告 + 自动按长度降序规避
- **零命中映射** → 单独列表。⚠️ **这是最需要你看的**：slug 是猜的，猜错就零命中（中文名仍会正确改名）
- **CSV 乱码检测** → 若 `rename-map.csv` 编码不对直接中止
- **自动备份** → `mds/_rename-backup/`

### 编码说明

映射数据放在 CSV 而不是 .ps1 里，
因为 **PowerShell 5.1 读无 BOM 的 .ps1 会按 ANSI 处理，中文会变乱码**。
`Import-Csv -Encoding UTF8` 绕开这个问题。脚本正文保持纯 ASCII。

---

## 五、原本要 grep 的 6 项，脚本自动化了 5 项

| # | 原本需要 grep | 现在的做法 |
|---|---|---|
| 1 | 导出 8 章全部真实 slug | 脚本的**零命中报告** |
| 2 | 统计子阶段名出现次数 | 守卫 1 / 2 自动验证 |
| 3 | 导出模块名精确出现次数 | dry-run 报告逐行显示 |
| 4 | 确认 `工业史定位` 不含模块名 | 守卫 4 |
| 5 | 检索 `UMV` | CSV 已含 `UMV前夜 → 终局阶段` |
| 6 | 备份 | 自动备份到 `mds/_rename-backup/` |

> 💡 剩下的只有「slug 是否猜对」，而这恰好就是零命中报告要告诉你的东西。

---

## 六、诚实度标注

| 内容 | 置信度 |
|---|---|
| 字段结构（§零） | ✅ **源码已验证**（`mds/04_MV.md` L30–L92 实读） |
| 「工业史定位不含模块名」 | ✅ **已验证 ch04**；🟢 ch06–13 同构待验 |
| 139 个中文模块名 | 🟢 **高置信**（来自本 AI 先前 PLAN 文档） |
| 新 slug | 🟢 **本次拟定**，无冲突性 |
| 原 slug | ⚠️ **未核实**（除 ch06 槽 00 的 `vacuum_tube`） |
| 单模块 141 处 | 🟢 基于 ch04 结构推算，⚠️ `玩家行动说明` 分布未全验 |
| ch06–13 章节行数 8530 | ✅ 已验证 |
| 脚本本身 | ⚠️ **未运行过**（AI 无 shell），需 dry-run 首测 |

---

## 七、建议的应用顺序

1. **先 dry run**，看零命中报告
2. **ch11 试点**（12 个改名，8 个零改动，全包最少）—— 验证流程本身
3. ch11 通过后再 `-Apply` 全量
4. **一次一章**，每章改完验证：任务数 400、questId 无重复、签名无断裂

> 💡 **建议 ch11 作为试点章** —— 改写量最小，且 8 个模块零改动，
> 容易对比出「哪些地方该改、哪些不该改」。

---

## 七、脚本实测后的重大修正（2026-xx）

上面 §四~§六 是初版设计。**实际去源码核实后发现两个致命问题，已修复。**

### 🔴 问题 1：旧 slug 全是猜的

初版把旧 slug 硬编码进 CSV。但**旧 slug 除 `晶体管` 外一个都没核实过**。
猜错的后果是**静默失败**：中文名改了，slug 纹丝不动，不报任何错。

**已改为脚本自动发现**：

```
文档签名：06-001 / 第 06 章 EV / transistors
                └── 章节 ──┘   └── slug ──┘

正则提取全文所有 slug → 去重 → 保序
文档顺序 == 模块顺序  ⇒  第 i 个 slug 配 CSV 第 i 行
```

脚本会先断言 **发现的 slug 数 == CSV 行数（应为 20）**，
不等则**拒绝执行**，而不是猜。

> ✅ 猜的东西从 139 个降到 **0 个**。

### 🔴 问题 2：8 处 `X 前夜` 少了空格

去源码核实 160 个模块名时发现，源文件里 `X 前夜` **全部带一个空格**：

| 源文件（✅ 实测） | 初版 CSV（❌ 错） | 章 |
|---|---|---|
| `IV 前夜` | `IV前夜` | 06 |
| `LuV 前夜` | `LuV前夜` | 07 |
| `ZPM 前夜` | `ZPM前夜` | 08 |
| `UV 前夜` | `UV前夜` | 09 |
| `UHV 前夜` | `UHV前夜` | 10 |
| `UEV 前夜` | `UEV前夜` | 11 |
| `UIV 前夜` | `UIV前夜` | 12 |
| `UMV 前夜` | `UMV前夜` | 13 |

**这 8 行全部会零命中** —— 因为 `IV前夜` 不是 `IV 前夜` 的子串。
已全部修正。其余 152 个模块名**逐条对源码核实，全部正确**。

### 🔴 问题 3：slug ≠ 检测对象

此前文档里记的「ch06 槽 02 = `gtceu:vacuum_tube`」是**检测对象**；
而 `晶体管` 的**模块 slug 实测是 `transistors`**。

两个概念混了。CSV 里已彻底分离：`检测对象` 不参与改名。

---

## 八、修正后的守卫（5 道）

| # | 守卫 | 挡什么 |
|---|---|---|
| 1 | 发现的 slug 数 == CSV 行数 | 配对错乱 |
| 2 | `维护与控制` 计数不变 | 铁律 1：裸词替换毁子阶段 |
| 3 | `规模化扩产` 计数不变 | 同上 |
| 4 | `任务编号` 计数不变 | 任务数被改坏 |
| 5 | `工业史定位` 计数不变 | 字段结构损坏 |

任一不满足 → **该文件拒绝写入**。自动备份到 `mds/_rename-backup/`。

### 编码处理

`.ps1` 正文**纯 ASCII**，中文用码点构造（如 `维护与控制` = `0x7EF4,0x62A4,0x4E0E,0x63A7,0x5236`）。
因为 **PowerShell 5.1 读无 BOM 的 .ps1 会按 ANSI 处理，中文会乱码**。
映射数据放 CSV，用 `Import-Csv -Encoding UTF8` 读。

---

## 九、诚实度标注（最终版）

| 内容 | 置信度 |
|---|---|
| **160 个中文模块名** | ✅ **已对源码逐条核实**（8 章 `章节内部结构` 段） |
| **字段结构（§零）** | ✅ **已验证**（`04_MV.md` L30–L92 + `06_EV.md` L28–L51） |
| 旧 slug | ✅ **不再需要猜**（脚本自动发现 + 数量断言） |
| 新 slug | 🟢 **本次拟定**，ASCII、无冲突 |
| 「工业史定位」不含模块名 | ✅ **ch04 + ch06 均已验证** |
| 单模块 141 处 | 🟢 推算，⚠️ `玩家行动说明` 分布未全验 |
| 脚本本身 | ⚠️ **未运行过**（AI 无 shell），必须先 dry-run |