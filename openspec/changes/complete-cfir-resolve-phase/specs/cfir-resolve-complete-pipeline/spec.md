## 新增需求

### 需求:CFIR_RESOLVE 必须完整覆盖全部子阶段
系统必须提供 `IMPORTS`、`SUPER_TYPES`、`TYPES`、`STATUS`、`EXTENSIONS`、`IMPLICIT_TYPES`、`BODY_RESOLVE`、`CHECKERS` 的正式实现，禁止以仅推进 `resolvePhase` 的空处理器替代完整语义行为。

#### 场景:推进到 CHECKERS 时完整执行阶段语义
- **当** 对包含类型声明、extend 声明、函数体表达式的 `CfirFile` 执行 `processFile` 或 `resolveTo(..., CHECKERS)`
- **那么** 每个声明必须按阶段顺序完成语义处理并达到 `CHECKERS`，且各阶段必须产出对应语义结果或诊断，而不是仅更新阶段标记

### 需求:总量解析与按需解析必须满足相同阶段契约
系统必须保证 total resolve 与 lazy resolve 在同一目标阶段下满足一致的前置条件、可见性边界和幂等性契约，禁止出现同一输入在两种模式下语义结果不一致。

#### 场景:同一声明在两种解析模式下结果一致
- **当** 对同一 `CfirDeclaration` 分别通过 `CfirTotalResolveProcessor` 与 facade 按需解析到 `BODY_RESOLVE`
- **那么** 声明阶段、已绑定类型/引用信息和诊断集合必须一致

### 需求:阶段处理器注册必须为正式实现而非最小兼容层
系统必须在 `CfirResolveComponentsRegistrar` 注册正式处理器与正式 provider 组合，禁止默认将 minimal 兼容路径作为主执行路径。

#### 场景:标准 session 装配使用正式组件
- **当** 创建 source session 并注册 resolve 组件
- **那么** registry 中必须存在全阶段正式处理器映射，且 provider/symbol provider/extend provider 不是“仅空实现即可通过阶段推进”的占位配置

## 修改需求

## 移除需求
