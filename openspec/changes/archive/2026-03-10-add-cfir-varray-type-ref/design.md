## 上下文

仓库当前对 `VArray<T, $N>` 的支持是不连续的：
- `psi/src/.../CangJieParsing.kt` 已提供 `VArray` 语法；
- `psi/src/.../CjVArrayType.kt` 已提供专用 PSI 节点；
- `cfir/cfir-cones/.../ConeArrayType.kt` 已定义 `ConeVArrayType`；
- 但 `cfir/cfir-tree/.../CfirTypeRef.kt` 还没有对应的语法层 type ref，`PsiConversionUtils.kt` 也没有 `CjVArrayType` 分支。

这使得合法的 `VArray` 类型在 Raw CFIR 构建阶段被错误降级为 `CfirErrorTypeRef("Unsupported type element: CjVArrayType")`。由于 rawBuilder golden file 正是用来验证第 6 阶段输出稳定性的，这一缺口必须在 Raw CFIR 层补齐，而不是留待后续 resolve 阶段“顺便修复”。同时，`openspec/specs/raw-cfir-implementation/spec.md` 中仍有“raw `CfirTypeRef` 全为 `CfirUserTypeRef`”之类的旧描述；该表述在 `CfirBasicTypeRef` 引入后已经过时，也会与本次 `VArray` 建模相冲突，因此需要在实现时同步校正。

本变更的约束是：
- 不改变 parser、PSI 或 cone 层既有 `VArray` 设计；
- 不在本变更中引入新的 `CFIR_RESOLVE` 规则、常量求值逻辑或运行时布局语义；
- 保持其他 `CfirTypeRef` 输出格式稳定，避免无关 golden file 漂移。

## 目标 / 非目标

**目标：**
- 在 `cfir-tree` 中为 `VArray<T, $N>` 提供专用的 Raw CFIR 类型引用表示。
- 在 `psi2cfir` 中把 `CjVArrayType` lowering 为对应的 CFIR type ref，而不是错误占位。
- 让 rawBuilder 输出保留 `VArray` 的元素类型和尺寸字面量信息，足以支持后续 resolve 映射到 `ConeVArrayType`。
- 为 `VArray` 在函数签名、属性、typealias 和嵌套类型位置补充回归测试。

**非目标：**
- 不在本变更中直接构造 `CfirResolvedTypeRef(ConeVArrayType)`。
- 不扩展 analysis API、序列化格式、CHIR 或 codegen。
- 不借机重构现有 `CfirTypeRef` 深层 transform 机制；若存在更广泛的 type-ref 变换问题，另行处理。

## 决策

### 决策 1：新增专用 `CfirVArrayTypeRef`，而不是复用 `CfirUserTypeRef`

**选择：**
- 在 `CfirTypeRef.kt` 中新增 `CfirVArrayTypeRef`；
- 该节点显式保存：元素类型引用、尺寸字面量，以及公共 `source`。

**原因：**
- `VArray<T, $N>` 的第二个参数是编译期尺寸字面量，而不是类型实参；复用 `CfirUserTypeRef(typeArguments=...)` 会混淆“类型参数”和“值参数”的边界。
- 仓库的语义层已存在 `ConeVArrayType`，Raw CFIR 层引入对应语法节点，能让第 6 阶段与后续类型语义一一对齐。

**备选方案：**
- 方案 A：复用 `CfirUserTypeRef`，把 `VArray` 当成普通泛型类型。拒绝原因：无法正确表达 `$N` 的非类型参数语义，也会让渲染与后续 resolve 语义变得含糊。
- 方案 B：在 rawBuilder 中直接生成 `CfirResolvedTypeRef(ConeVArrayType)`。拒绝原因：Raw CFIR 阶段仍应保持“未解析”属性，且元素类型本身也可能尚未 resolve。

### 决策 2：Raw CFIR 保存尺寸字面量文本，而不是提前承诺数值语义

**选择：**
- `CfirVArrayTypeRef` 保存来自 PSI 的整数 token 文本（例如 `4`），渲染时以 `VArray<..., $4>` 的 canonical 形式输出。

**原因：**
- 语法已经要求第二参数必须是 `"$" integerLiteral`，Raw CFIR 只需保留语法信息，不应在此阶段承担溢出检查或常量求值责任。
- 使用文本而非 `Long` 可避免在 rawBuilder 里过早引入字面量解析失败、范围检查和进制/分隔符等语义细节。

**备选方案：**
- 方案 A：在 rawBuilder 中把尺寸立即解析为 `Long`。拒绝原因：会把不属于第 6 阶段的语义约束前移，并增加非必要的失败模式。

### 决策 3：影响面限制在 `cfir-tree` 表达层与 `psi2cfir` lowering 层

**选择：**
- 同步更新 `CfirVisitor.kt` 与 `CfirRenderer.kt` 对新 type ref 的覆盖；
- 在 `PsiConversionUtils.kt` 中新增 `CjVArrayType` 分支，并复用现有类型转换函数构建元素类型；
- malformed PSI（缺失元素类型或尺寸字面量）继续返回 `CfirErrorTypeRef`，保持 rawBuilder 容错模式一致。

**原因：**
- 当前缺口就发生在 PSI → Raw CFIR 边界和 type-ref 表达层；扩大到 analysis/resolve 不会帮助用户更早看到正确的 rawBuilder 输出，反而会增加交付面。

**备选方案：**
- 方案 A：同时补 analysis 或 resolve 针对 `VArray` 的语义规则。拒绝原因：没有证据显示这些模块已经消费该能力，且超出当前问题范围。

### 决策 4：以 rawBuilder golden 回归作为验收主线

**选择：**
- 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder` 中新增或扩展覆盖 `VArray` 的样例；
- 验证渲染输出既不再出现 `Unsupported type element: CjVArrayType`，也能稳定展示元素类型与 `$N`。

**原因：**
- 该缺口本质是 Raw CFIR 输出错误，golden file 是当前仓库对第 6 阶段最直接的验收机制。

## 风险 / 权衡

- [风险] 现有 `CfirTypeRef` 子类对 `transformChildren` 的深度遍历并不完备，新节点若额外做深层 transform 可能与既有模式不一致 → **缓解**：本变更先对齐当前 type-ref 设计习惯，只解决建模、渲染与 lowering 缺口。
- [风险] `VArray` 的 canonical 渲染格式若设计不稳，会导致后续 golden 再次漂移 → **缓解**：在设计中固定为 `VArray<element, $size>`，并通过测试锁定输出。
- [权衡] 保存尺寸文本会把数值校验推迟到 resolve，但这与当前分阶段架构一致，并能减少 Raw CFIR 过度语义化。
- [风险] 仅补代码而不修正文档/规范中的过时 type-ref 描述，会导致后续实现者误以为 Raw CFIR 仍只能承载 `CfirUserTypeRef` → **缓解**：把规范文字对齐纳入任务清单，与代码改动一并完成。

## 迁移计划

1. 先在 `cfir-tree` 中引入 `CfirVArrayTypeRef`，补齐 visitor / renderer 覆盖。
2. 再在 `PsiConversionUtils.kt` 中增加 `CjVArrayType` lowering。
3. 添加/更新 rawBuilder 样例与 golden file，覆盖常见声明位置和嵌套元素类型。
4. 运行 `:cfir:raw-cfir:psi2cfir:test` 验证输出稳定，再决定是否需要补充更细粒度测试。

## 开放问题

- 尺寸字面量是否需要在 Raw CFIR 层保留原始文本（含可能的分隔符形式）还是做轻量 canonical 化，需要在实现时以现有测试风格为准。
- 如果后续 `CFIR_RESOLVE` 需要统一的“非类型参数”抽象，`CfirVArrayTypeRef` 是否应演化为更通用的值级类型参数容器，不在本变更内处理。
