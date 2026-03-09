## 1. CFIR 类型引用建模补齐

- [x] 1.1 在 `cfir/cfir-tree/src/org/cangjie/cfir/types/CfirTypeRef.kt` 中新增 `VArray` 专用 type ref，并明确其元素类型与尺寸字面量字段
- [x] 1.2 在 `cfir/cfir-tree/src/org/cangjie/cfir/visitors/CfirVisitor.kt` 中补齐 `VArray` type ref 的访问入口，确保与现有 type-ref 分派一致
- [x] 1.3 更新 `cfir/cfir-tree/src/org/cangjie/cfir/renderer/CfirRenderer.kt`，让 rawBuilder 输出稳定渲染 `VArray<..., $N>`

## 2. PSI → Raw CFIR lowering

- [x] 2.1 在 `cfir/raw-cfir/psi2cfir/src/org/cangjie/cfir/builder/PsiConversionUtils.kt` 中增加 `CjVArrayType` 分支，并复用现有类型转换逻辑构建元素类型
- [x] 2.2 为 malformed `CjVArrayType` 保持一致的降级策略：缺失关键 PSI 片段时返回可诊断的 `CfirErrorTypeRef`
- [x] 2.3 核对新 lowering 不影响 `CjBasicType`、`CjUserType`、`CjFunctionType` 与 `CjTupleType` 的既有输出

## 3. RawBuilder 测试覆盖

- [x] 3.1 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder` 中新增或扩展 `VArray` 样例，覆盖函数签名、属性和 `typealias`
- [x] 3.2 增加至少一个嵌套元素类型样例（如用户类型、元组类型或函数类型作为 `VArray` 元素）
- [x] 3.3 更新对应 golden file，确认不再出现 `Unsupported type element: CjVArrayType`

## 4. 验证与文档对齐

- [ ] 4.1 运行 `:cfir:raw-cfir:psi2cfir:test` 验证 rawBuilder 与相关 golden file 全部通过
- [ ] 4.2 更新 `openspec/specs/raw-cfir-implementation/spec.md` 与 `README.md` 中的相关描述，移除“raw type ref 全为 CfirUserTypeRef”等过时表述，并反映 `VArray` 已纳入 type-ref 建模范围
