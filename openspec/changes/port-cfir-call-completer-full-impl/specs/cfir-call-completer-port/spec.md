## 新增需求

### 需求:CfirCallCompleter 完整移植
系统必须在 CFIR 体系内提供与 Kotlin FIR `FirCallCompleter` 语义等价的 `CfirCallCompleter`，且实现必须包含调用补全过程所需的完整逻辑，禁止以最小实现替代。

#### 场景:补全过程可独立编译并可实例化
- **当** 构建系统编译包含 `CfirCallCompleter` 及其依赖的相关模块
- **那么** 编译必须成功，且调用补全器可在 CFIR 上下文中被实例化而不依赖 K1 包

#### 场景:候选补全语义与上游目标一致
- **当** 输入触发调用候选补全与约束补全流程
- **那么** `CfirCallCompleter` 必须执行与迁移目标一致的补全阶段，并生成可用于后续解析阶段的补全结果

### 需求:禁止自主实现替代上游迁移
系统必须以 Kotlin 编译器源码为唯一实现基线，禁止自主设计或重写 `CfirCallCompleter` 的核心方法逻辑。

#### 场景:实现来源可追溯
- **当** 审查 `CfirCallCompleter` 及其核心协作实现
- **那么** 每个核心方法都必须可映射到上游对应方法或明确标注“仅适配改动及原因”

### 需求:本阶段禁止接入 ExpressionsResolveTransformer
系统在本变更范围内必须保持 `CFirExpressionsResolveTransformer` 的现有调用路径不变，禁止新增 `CfirCallCompleter` 的执行接线。

#### 场景:变更完成后 transformer 行为不变
- **当** 对比变更前后 `CFirExpressionsResolveTransformer` 的调用补全接线点
- **那么** 不得出现新增或替换为 `CfirCallCompleter` 的调用路径

## 修改需求

- 无。

## 移除需求

- 无。
