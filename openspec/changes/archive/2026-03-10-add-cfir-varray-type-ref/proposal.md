## 为什么

当前仓库已经在 PSI/语法层支持 `VArray<T, $N>`，并且在 cone 语义类型层已有 `ConeVArrayType`，但 Raw CFIR 的语法类型引用层仍缺少对应表达，导致 `CjVArrayType` 在 `psi2cfir` 中被降级为 `Unsupported type element`。这会让合法的仓颉定长数组类型在第 6 阶段就失真为错误类型引用，阻碍 rawBuilder 测试与后续 `CFIR_RESOLVE` 的持续推进。

## 变更内容

- 在 `cfir-tree` 中补齐 `CfirTypeRef` 对 `VArray` 的语法层表达，并同步接入 visitor / renderer / 必要的转换路径。
- 在 `cfir/raw-cfir/psi2cfir` 中新增 `CjVArrayType` → CFIR `VArray` type ref 的 lowering，保留元素类型引用与编译期尺寸字面量。
- 补充 rawBuilder 测试样例与 golden file，覆盖函数签名、属性、typealias 与嵌套类型位置中的 `VArray`。
- 约束本变更只补齐 Raw CFIR 建模与输出稳定性，不在本次范围内引入新的 resolve 语义、运行时布局规则或 codegen 行为。

## 功能 (Capabilities)

### 新增功能

### 修改功能
- `raw-cfir-implementation`: Raw CFIR 类型引用建模与 PSI → CFIR 转换必须覆盖仓颉特有的 `VArray<T, $N>`。

## 影响

- 受影响模块：`cfir/cfir-tree`、`cfir/raw-cfir/psi2cfir`。
- 受影响代码：`CfirTypeRef.kt`、`CfirVisitor.kt`、`CfirRenderer.kt`、`PsiConversionUtils.kt` 以及 rawBuilder testData / golden 输出。
- 受影响规范：`openspec/specs/raw-cfir-implementation/spec.md` 需要增加对 `VArray` Raw CFIR 建模的约束，并清理“raw type ref 全为 `CfirUserTypeRef`”等与当前实现状态不一致的陈旧描述。
- 兼容性：不改变 parser、PSI 或 cone 层既有 `VArray` 设计；新增能力应保持其他类型引用输出稳定。
