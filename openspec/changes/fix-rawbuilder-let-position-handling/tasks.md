## 1. 范围收敛与基线确认

- [ ] 1.1 复核 `classWithMembers`、`classWithTypeParameters`、`structDeclaration` 当前 golden 中成员字段退化为 `<error-declaration>` 的现象，并记录预期修正后的文本差异
- [ ] 1.2 设计 pattern variable 的独立声明节点：保留现有 `CfirVariable(name)` 不变，并明确 `CfirPatternVariable(pattern)` 的最小字段与 symbol/visitor 需求

## 2. CfirPatternVariable 建模与 rawBuilder 接入

- [ ] 2.1 在 `cfir-tree` 中新增 `CfirPatternVariable : CfirCallableDeclaration`
- [ ] 2.2 同步补齐 `CfirPatternVariable` 对应的 visitor / renderer / symbol 最小支持：现有 `CfirVariable` 保持按名称渲染，`CfirPatternVariable` 改走 pattern 渲染路径
- [ ] 2.3 在 `PsiRawCfirBuilder.convertDeclaration()` 中补充 `CjFieldVariable` 的 dispatch，并将其接入现有 `CfirVariable` 转换路径，确保 `name`、`isVar`、`typeReference`、`initializer` 与 source mapping 被正确保留
- [ ] 2.4 为 `CfirPatternVariable` 设计 derived query（如 bindings / allPatternDeclarations）而不是持久化子变量列表
- [ ] 2.5 保持文件级/局部 `CjPatternVariable` 的完整 lowering 不在本次实现范围内，但禁止继续用具名变量或 property 语义掩盖其独立建模

## 3. 位置敏感回归测试

- [ ] 3.1 更新 `classWithMembers`、`classWithTypeParameters`、`structDeclaration` 的 rawBuilder / lazyBodies golden，使类体字段不再输出 `<error-declaration>`
- [ ] 3.2 新增至少一个类体成员顺序回归用例，覆盖字段位于函数或构造器前后时的输出稳定性
- [ ] 3.3 如新增 testData 触发 generated tests 变化，重新生成并校验相关 `tests-gen` 文件

## 4. 验证与文档同步

- [ ] 4.1 运行 `:cfir:raw-cfir:psi2cfir:test`，验证 rawBuilder、lazyBodies by-ast、lazyBodies by-stub 全部通过
- [ ] 4.2 更新 `README.md` 与 `openspec/specs/raw-cfir-implementation/spec.md` 的说明，明确“保留现有 `CfirVariable` + 新增 `CfirPatternVariable`”的结构，以及 `CfirPatternVariable` 采用“完整 pattern + 派生 bindings”的设计
