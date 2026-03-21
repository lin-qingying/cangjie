## 新增需求

### 需求：CfirCallCompleter 完整移植（基于 K2/FIR，无 K1 依赖）
系统必须在 CFIR 体系内提供与 Kotlin K2/FIR `FirCallCompleter` 语义等价的 `CfirCallCompleter`，且实现必须包含调用补全过程所需的完整逻辑，禁止以最小实现替代，禁止引入任何 `resolution.common` K1 约束体系依赖。

#### 场景：补全过程可独立编译并可实例化
- **当** 构建系统编译包含 `CfirCallCompleter` 及其依赖的相关模块
- **那么** 编译必须成功，且满足以下全部条件：
    - `CfirCallCompleter` 可在 CFIR 上下文中被实例化
    - 不依赖任何 `org.jetbrains.kotlin.resolve.calls.inference`（K1）路径下的符号
    - 约束操作路径经由 `CfirInferenceSession` / `CfirInferenceContext` 体系执行，不经由 K1 `ConstraintStorage` 路径

#### 场景：候选补全阶段顺序与上游 K2 目标一致
- **当** 输入触发调用候选补全流程
- **那么** `CfirCallCompleter` 必须按照与 K2/FIR 上游一致的阶段顺序执行补全（候选收集 → 约束求解 → 推断会话提交），并生成可用于后续解析阶段的补全结果；各阶段入参与出参结构须可映射到上游同名方法

#### 场景：K1 调用点已全部改写为 K2 推断会话操作
- **当** 静态扫描 `CfirCallCompleter` 及其直接依赖文件的导入与类型引用
- **那么** 不得出现 `org.jetbrains.kotlin.resolve.calls.inference` 包下任何符号的直接引用；原 K2/FIR 源码中对 K1 API（如 `asReadOnlyStorage`、`currentStorage`、`buildCurrentSubstitutor`）的调用点均已改写为等价的 K2 推断会话操作，且每个改写点在追溯矩阵中有 `K1调用改写` 状态记录

### 需求：禁止自主实现替代上游迁移
系统必须以 Kotlin K2/FIR 编译器源码为唯一实现基线，禁止自主设计或重写 `CfirCallCompleter` 的核心方法逻辑。

#### 场景：实现来源可追溯
- **当** 审查 `CfirCallCompleter` 及其核心协作实现
- **那么** 每个核心方法都必须可映射到上游 K2/FIR 对应方法，或明确标注"仅适配改动（K1调用改写/包名映射/语法适配）及原因"；不得存在无上游来源依据的核心逻辑

### 需求：本阶段禁止接入 ExpressionsResolveTransformer
系统在本变更范围内必须保持 `CFirExpressionsResolveTransformer` 的现有调用路径不变，禁止新增 `CfirCallCompleter` 的执行接线。

#### 场景：变更完成后 transformer 行为不变
- **当** 对比变更前后 `CFirExpressionsResolveTransformer` 的调用补全接线点（diff 检查）
- **那么** 不得出现新增或替换为 `CfirCallCompleter` 的调用路径

## 修改需求

- 无。

## 移除需求

- 无。