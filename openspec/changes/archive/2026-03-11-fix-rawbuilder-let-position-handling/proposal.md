## 为什么

当前 `cfir/raw-cfir/psi2cfir/testData/rawBuilder/declarations/classWithMembers.cj` 的源码已经包含合法的类成员字段声明：`var x: Int64 = 0` 与 `let name: String = "hello"`。但对应的 `classWithMembers.txt` / `classWithMembers.lazyBodies.txt` 仍将这两个成员都渲染为 `let <error-declaration>: <implicit>`。同类退化也出现在 `classWithTypeParameters`、`structDeclaration` 等类体成员样例中。

代码路径已经表明这不是 renderer 文本问题，而是 PSI 覆盖缺口：`CjAbstractClassBody.declarations` 会返回 `FIELD` 成员（`CjFieldVariable`），但 `PsiRawCfirBuilder.convertDeclaration()` 只处理 `CjProperty`，没有处理 `CjFieldVariable`。同时，仓库里的 `Variable`、`Field`、`Property` 本来就是不同概念：`CjFieldVariable` 表示类成员字段，`CjPatternVariable` 表示模式变量，`CjProperty` 则对应带 property/accessor 语义的 `PropDecl`。如果不先通过提案明确修复边界，后续实现很容易一边误扩成“所有位置的 let 声明都要支持”，一边又把成员字段错误建模成 `Property`。

## 变更内容

- 修复 Raw CFIR rawBuilder 对类成员字段与模式变量的建模缺口：保留现有具名 `CfirVariable` 实现不变，用它继续承载具名单一变量；另外新增 `CfirPatternVariable : CfirCallableDeclaration`，专门表示携带完整 `CfirPattern` 的模式变量声明。这样既能让类/接口/结构体/枚举体内的 `CjFieldVariable` 不再统一退化为 `<error-declaration>`，也能为后续 pattern variable lowering 预留独立结构。
- 为类体中的成员字段补齐位置敏感回归测试，覆盖字段出现在函数前、构造器前后、不同成员顺序中的稳定输出。
- 明确本次变更的边界：本次会先保留现有 `CfirVariable` 语义不变，并补齐新的 `CfirPatternVariable` 结构与类成员字段 lowering；对文件级或局部 `CjPatternVariable` 的完整 lowering 仍保持收敛。`CfirPatternVariable` 会保留完整 pattern 结构，但不会在 raw CFIR 中把 0..N 个绑定再复制成持久子变量列表。
- 更新 `README.md` 与 OpenSpec `raw-cfir-implementation` 规范，记录成员字段 lowering 的能力边界与测试要求。

## 功能 (Capabilities)

### 新增功能

### 修改功能
- `raw-cfir-implementation`: Raw CFIR 必须保留现有具名 `CfirVariable` 作为单一变量声明表示，并新增独立的 `CfirPatternVariable` 来承载模式变量声明；rawBuilder 在类成员场景下应继续使用具名变量建模，而不是把成员字段直接混同为 `Property`。

## 影响

- 受影响模块：`cfir/raw-cfir/psi2cfir`（`PsiRawCfirBuilder`、rawBuilder/lazyBodies testData、generated tests）与 `cfir/cfir-tree`（新增 `CfirPatternVariable`、对应 symbols、visitor、renderer）。
- 受影响测试：`classWithMembers`、`classWithTypeParameters`、`structDeclaration` 及新增的类体成员位置回归用例。
- 受影响规范：`openspec/specs/raw-cfir-implementation/spec.md` 需要补充“类体成员字段 lowering”相关要求。
- 受影响文档：`README.md` 需要同步记录该提案及其明确的非目标范围。
