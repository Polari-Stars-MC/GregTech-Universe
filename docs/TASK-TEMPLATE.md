# 任务需求列表（Template）

> 本文件是 `tasks/` 下各模块任务文档的**格式模板**。
> 复制本模板 → 填入具体任务 → 存为 `docs/tasks/<模块>.md`。
>
> 每条任务**必须**标注：边界档位、涉及文件、依赖关系、验证方式。
> 标注不全的任务不允许开工。

---

## 文件命名

```
docs/tasks/core.md        → GTU-core-xxx
docs/tasks/modpacks.md    → GTU-modpacks-xxx
docs/tasks/physics.md     → GTU-physics-xxx
```

---

## 模板

````markdown
# <模块> 任务需求

> 模块：`<mod>`（modId = `<modId>`）
> 源码目录：`src/main/<mod>/`　资源目录：`src/res/<mod>/`
> 依据文档：[WORKFLOW.md](../WORKFLOW.md) · [AI-BOUNDARY.md](../AI-BOUNDARY.md) · [CODE-STYLE.md](../CODE-STYLE.md)

## 模块约束

- 依赖方向：`modpacks → core`、`modpacks → physics`；`core` 与 `physics` 互不依赖
- 禁止内容：`<本模块特有禁令>`
- 本模块热点文件：`<改动最频繁、最需小心的文件>`

## 任务一览

| ID | 标题 | 档位 | 依赖 | 状态 |
|---|---|---|---|---|
| GTU-<mod>-001 | xxx | 🟢 | — | ⬜ |
| GTU-<mod>-002 | xxx | 🟡 | 001 | ⬜ |

状态图例：⬜ 未开始　🔄 进行中　✅ 完成　❌ 阻塞　⏸️ 待人工确认

---

## GTU-<mod>-001 — <标题>

### 需求

<用户视角描述：做完之后什么变了。不要写实现细节。>

### 验收标准

- [ ] <可验证的条件 1>
- [ ] <可验证的条件 2>

### 边界

| 项 | 值 |
|---|---|
| 档位 | 🟢 / 🟡 / 🔴 |
| 需人工确认项 | `<若无填「无」>` |

### 涉及文件

| 动作 | 路径 | 说明 |
|---|---|---|
| `+` | `src/main/<mod>/.../Foo.java` | 新增类 |
| `~` | `src/main/<mod>/.../Bar.java` | 修改第 N 行：xxx |
| `+` | `src/res/<mod>/assets/.../foo.json` | 模型 |

### 实现要点

1. <照抄哪个已有实现的哪种写法>
2. <注册放哪个类的什么位置>
3. <需要注意的端/线程问题>

### 验证

- [ ] `gradlew :<mod>:compileJava`
- [ ] `gradlew :<mod>:runData`（若动资源）
- [ ] 人工进游戏验证：<具体场景>

### 依赖 / 风险

- 前置：<ID 或「无」>
- 后续可解锁：<ID>
- 风险：<影响存档/性能/兼容的点，或「无」>

---
````

---

## 边界档位速查

| 档位 | 含义 | 开工前动作 |
|---|---|---|
| 🟢 | 只动 `src/main/<mod>/**`、`src/res/<mod>/` 新增资源、新增 mixin 类 | 直接进 S2 |
| 🟡 | 动版本字段、依赖、AT、已有 mixin 注入点、已有注册 ID | 先出精确 diff 到终端，等人工确认 |
| 🔴 | 动 `src/generated/**`、`templates/**`、二进制、复合构建、存档 | 停止实施，只出方案不落盘 |

详见 [AI-BOUNDARY.md](../AI-BOUNDARY.md)。

---

## 拆分原则

1. **一个 ID 只做一件事**，能在一次 `compileJava` 里说清结果。
2. **先底层后上层**：注册类 → 业务类 → 资源文件，同一功能放一个 ID 或紧邻编号。
3. **有 🟡 的独立成 ID**，不与 🟢 混在一起，避免整条任务被卡。
4. **先做被依赖的**，编号小的通常是前置。
5. **标不清边界就先不写**，回去读 `src/main/` 同类实现。
