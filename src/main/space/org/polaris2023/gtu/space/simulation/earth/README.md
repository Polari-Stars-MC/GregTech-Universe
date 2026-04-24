# Earth Projection Entry Points

`space` 模块内的地球投影入口统一从 `EarthWorldgenBridge` 进入。

## Core entry points

- `EarthWorldgenBridge.project(...)`
  - 把原始方块坐标映射到投影坐标。
- `EarthWorldgenBridge.projectChunk(...)`
  - 获取区块级投影状态。
- `EarthWorldgenBridge.projectedChunkWindows(...)`
  - 获取区块在接缝处拆分后的投影窗口；跨反经线时会返回两个窗口。
- `EarthWorldgenBridge.shouldGenerateChunk(...)`
  - future worldgen hook 可用的统一区块生成判定。
- `EarthWorldgenBridge.shouldGenerateTerrain(...)`
  - future worldgen hook 可用的统一方块生成判定。
- `EarthWorldgenBridge.sampleProjected(...)`
  - 对 density function 进行投影采样。
- `EarthWorldgenBridge.resolveOutOfBoundsBlock(...)`
  - 越界时的统一 fallback 方块。

## Seamless travel

- `EarthSeamlessTravelService.wrapPlayerIfNeeded(...)`
  - 玩家越过 `X` 向投影接缝时，将其无缝包裹到另一侧。
- `EarthClientSeamController`
  - 客户端接缝状态机。
  - 在接缝预览带内创建并同步镜像玩家代理。
  - 在接缝切换带内将主相机切到镜像代理实体上。
- `EarthClientPlayerProxy`
  - 客户端镜像玩家代理实体。
- `EarthClientSeamState`
  - 客户端接缝状态快照，包含是否接近东西接缝、是否切换相机、wrap 后坐标等。

## Debug

- `EarthProjectionDebugHud`
  - 客户端 `F7` 开关。
  - 显示当前位置、wrap 后坐标、经纬度、距离接缝/极限的剩余距离、当前区块是否被接缝拆分，以及客户端接缝切换状态。
