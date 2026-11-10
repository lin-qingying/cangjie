# STUBS extend 成员 PSI 缺失：设计说明与分步方案（2026-11-09）

## 现象

`array.size` 解析成功，但成员符号 `CaCfirPropertySymbol` 的 `psi == null`：

- 快速文档为空（没有 PSI 可取文档/声明文本）；
- 转到声明不可用（没有导航目标）。

根因：extend 只从 `.cjo` 反序列化来，`CfirExtendImpl.source` 是构造期 `val`，反序列化路径恒为 null，
且无法事后补。

## 已核实的代码事实

1. `CfirDeserializedExtendProvider.buildIndex()` 通过
   `AbstractCfirDeserializedSymbolProvider.getTopLevelExtendDeclarations(package)` 取 extend；
   后者用 `declDeserializer.deserializeDecl(...)` 从 `.cjo` 构建，声明没有 PSI。
2. decompiled **文本**是保真的：`std.core.cjo.decompiled.text.cj:1468` 有
   `extend std.core.Array<T> <: Collection {` 与 `prop size`。
3. decompiled **stub PSI 保真**（已实测，std.core 共 97 个 extend）：Array<:Collection 的
   `receiverTypeReceiver="std.core.Array<T>"`、`superTypeListEntries=[Collection]`，
   文本为 `extend std.core.Array<T> <: Collection { ... }`。因此 PSI 层并不缺父类型。
4. 真正的阻塞是**隐式类型参数**：`extend<T> Array<T>` 的 `T` 不显式声明在 stub/PSI 中，
   从 stub 构建 extend 时（header 用二进制类型引用可绕过；成员如
   `iterator(): std.core.Iterator<T>` 仍要走 stub 类型反序列化）报
   `Class name must not be root: T`。`StubBasedCfirTypeDeserializer` 没有为 extend 建立
   隐式类型参数作用域。
5. 没有 extend 的包级索引：`CaStubSummaryBuilder` 跳过 `CangJieExtendStub`；
   `StubIndexService.indexExtend` 是空实现；provider 无按包枚举 extend 的入口。

## 分步方案（每步：红测试 → 实现 → 回归 → 子代理复审）

### 阶段 1：隐式类型参数作用域（已完成原型验证）
- 方案：不改 stub 布局；`StubBasedCfirTypeDeserializer` 增加 `additionalTypeParameters`，
  由 `StubBasedCfirExtendBuilder` 从 `.cjo` 反序列化 extend 的 `typeParameters`（实测 `[T]`）
  注入。extend 头部类型引用同样复用二进制已解析结果。
- P0 修复（结构键）：
- 新增 `LLExtendStructuralKeys`：完整键 = 接收者 + 父类型 + 成员签名（kind|name|参数个数），
  二进制与 PSI 两侧规则一致；同 key 的 PSI 声明以队列保存并逐个消费，不再“第一个 PSI 服务所有二进制 extend”；
- `LLPsiBackedExtendProvider` 只对二进制来源按完整键去重（源码 extend 原样保留）；
- 物化失败逐条 try/catch + log + 回退二进制；
- `getContainingFile` 委托 binaryProvider；
- 物化声明补齐 `status`（含 static）/`annotations`，成员补齐 `dispatchReceiverType`；
- 新增 parity 测试 `ExtendParityStubsBinaryTest`：`Int64`/`Float64`/`Iterator` 的 STUBS 与 BINARIES
  extend 数量、成员分组一致，`Float64.fromBits` 存在、`static`、带 PSI。

验证：`StubBasedCfirExtendPsiTest` 1/0——PSI 来源的 extend 同时具备
  `source != null`、`superTypeRefs=[Collection]`、`typeParameters=[T]`，且 `prop size` 的
  `source != null`。

### 阶段 2：stub → CFIR extend 构建（已完成原型）
- 红测试：从 stub PSI 构建 `extend Array<T> <: Collection` 的 `CfirExtend`，成员
  `iterator(): std.core.Iterator<T>` 的 `T` 必须解析为 extend 的隐式类型参数，而不是
  `Class name must not be root: T`。
- 修复：stub-based 反序列化上下文为 extend 建立隐式类型参数（从 receiver 的 class-like
  symbol 的 typeParameters 派生，或从 cjo metadata 的 extend typeParameters 读取），
  并让 `StubBasedCfirTypeDeserializer` 在 extend 成员类型中可见这些参数。
- 风险：与源码路径的 type parameter 作用域语义必须一致；需对照 `PsiRawCfirBuilder.convertExtend`。

- `StubBasedCfirExtendBuilder.build(extendPsi, binaryExtend)`：成员用
  `StubBasedCfirMemberDeserializer` 构建（成员自带 PSI），extend 自身 `source = CjExtend`。

### 阶段 3：包级索引与装配
- 新增 extend 的包级索引（当前 `CaStubSummaryBuilder` 跳过、`indexExtend` 空实现、
  provider 无按包枚举入口），供 provider 按包收集。
- STUBS 下 session extend provider 优先 stub 来源（带 PSI），与 `.cjo` 来源按
  `extensionSignatureKey()` 去重；BINARIES 不变。
- 风险：新增索引的版本/内存；std.core 有 97 个 extend。

### 阶段 4：装配与去重
- STUBS 下 session extend provider 优先 stub 来源（带 PSI），与 `.cjo` 来源按
  `extensionSignatureKey()` 去重；BINARIES 不变。
- 回归：analysis-api-cfir / low-level / decompiler-to-stubs / decompiler-to-psi / stubs /
  perf；`enumEntryShadowedByTypeName` 保持既有失败文本。

## 过渡方案（若需要立即可用）
B：仅对“声明无 PSI”的 extend 成员做导航/文档桥接（按 classId + 成员名在 decompiled stub PSI
中定位 `CjExtend`/`CfirExtend` 对应声明）。不改变解析，A 完成后移除桥接。

## 当前状态（2026-11-10）

已实现并P0 修复（结构键）：
- 新增 `LLExtendStructuralKeys`：完整键 = 接收者 + 父类型 + 成员签名（kind|name|参数个数），
  二进制与 PSI 两侧规则一致；同 key 的 PSI 声明以队列保存并逐个消费，不再“第一个 PSI 服务所有二进制 extend”；
- `LLPsiBackedExtendProvider` 只对二进制来源按完整键去重（源码 extend 原样保留）；
- 物化失败逐条 try/catch + log + 回退二进制；
- `getContainingFile` 委托 binaryProvider；
- 物化声明补齐 `status`（含 static）/`annotations`，成员补齐 `dispatchReceiverType`；
- 新增 parity 测试 `ExtendParityStubsBinaryTest`：`Int64`/`Float64`/`Iterator` 的 STUBS 与 BINARIES
  extend 数量、成员分组一致，`Float64.fromBits` 存在、`static`、带 PSI。

验证：
- `StubBasedCfirTypeDeserializer` 支持 `additionalTypeParameters`（注入 `.cjo` extend 的隐式 `T`）；
- `StubBasedCfirExtendBuilder`：以 `CjExtend` PSI 为 source/members 来源，类型引用与类型参数复用二进制结果；
- `LLExtendPsiMaterializer`：按「接收者 + 父类型」结构签名在包内 decompiled PSI 中配对并用 builder 物化；
  键需去掉 CFIR renderer 的 `struct `/`class ` 声明种类前缀才能与 PSI 文本匹配；
- `LLPsiBackedExtendProvider`：以物化声明建立索引（target/package/callable/structural），找不到 PSI 时回退二进制；
  源码 extend 原样保留、只对二进制来源去重；
- 接线：builtins session、binary library session、source session 组合层均在 STUBS 下启用物化；BINARIES 不变。

P0 修复（结构键）：
- 新增 `LLExtendStructuralKeys`：完整键 = 接收者 + 父类型 + 成员签名（kind|name|参数个数），
  二进制与 PSI 两侧规则一致；同 key 的 PSI 声明以队列保存并逐个消费，不再“第一个 PSI 服务所有二进制 extend”；
- `LLPsiBackedExtendProvider` 只对二进制来源按完整键去重（源码 extend 原样保留）；
- 物化失败逐条 try/catch + log + 回退二进制；
- `getContainingFile` 委托 binaryProvider；
- 物化声明补齐 `status`（含 static）/`annotations`，成员补齐 `dispatchReceiverType`；
- 新增 parity 测试 `ExtendParityStubsBinaryTest`：`Int64`/`Float64`/`Iterator` 的 STUBS 与 BINARIES
  extend 数量、成员分组一致，`Float64.fromBits` 存在、`static`、带 PSI。

验证：
- `ArraySizeStubsResolutionTest` 断言成员 `psi != null`：**1/0 绿**（修复前为红）；
- `StubBasedCfirExtendPsiTest`、`ExtendPsiDiscoveryTest`：1/0；
- 全量：analysis-api-cfir 1694/0；low-level 193/1（唯一失败为既有外部项
  `enumEntryShadowedByTypeName`，用禁用物化的 mutation 验证仍为同一失败文本）；
- decompiler-to-stubs 10/0、decompiler-to-psi 43/0、stubs 8/0；
- perf：CaAnalysisApiStatisticsTest 7/0/7.124s、CaSpanTimingTest 2/0/1.149s、
  CaSessionAndDeserializationStatisticsTest 1/0/0.017s。
