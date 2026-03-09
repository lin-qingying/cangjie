## 1. 覆盖范围收敛

- [x] 1.1 通过最小 parse/build 验证盘点 `PsiRawCfirBuilder` 中已存在的 recoverable missing-expression 分支，并将其映射到当前仓颉 parser 真实可恢复的源代码形态
- [x] 1.2 结合 Kotlin `rawBuilder/expressions` 参考用例，确定首批仓颉侧缺失表达式样例清单与命名规则

## 2. testData 与生成器对齐

- [x] 2.1 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder/expressions` 下新增缺失表达式 `.cj` 用例，并为主 rawBuilder suite 补齐 `*.txt` golden files
- [x] 2.2 调整 `TestGeneratorForPsi2Cfir.kt` 与生成测试代码，使 `RawCfirBuilderLazyBodiesByAstTestGenerated` / `RawCfirBuilderLazyBodiesByStubTestGenerated` 不再只扫描 `declarations/`，而是能发现 `expressions/` 目录
- [x] 2.3 为新增表达式用例补齐 `*.lazyBodies.txt` 基线，并确认 generated tests 的 all-files-present 等效校验能覆盖新目录

## 3. 恢复输出稳定化

- [x] 3.1 如果新增样例暴露出不稳定或缺失的恢复输出，对 `PsiRawCfirBuilder` 做最小范围修正，使其落入现有 `ERROR_EXPR(...)` / 空 block / 空结果约定
- [x] 3.2 如有必要，同步收敛 `CfirRenderer` 对 recoverable error output 的文本表现，避免 golden 文件在无语义变化时持续漂移

## 4. 验证与文档同步

- [x] 4.1 运行 `:cfir:raw-cfir:psi2cfir:test`，验证 rawBuilder、lazyBodies by-ast、lazyBodies by-stub 与 generated tests 全部通过
- [x] 4.2 更新 `README.md` 与 `openspec/specs/raw-cfir-implementation/spec.md`，反映 rawBuilder 表达式目录与缺失表达式回归覆盖的最新状态
