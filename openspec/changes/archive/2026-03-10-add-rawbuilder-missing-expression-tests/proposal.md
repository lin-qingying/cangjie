## 为什么

当前 `cfir/raw-cfir/psi2cfir/testData/rawBuilder` 只覆盖 `declarations/` 下的一组正常样例，没有任何“缺失表达式 / 不完整表达式”回归用例。与此同时，`PsiRawCfirBuilder` 已经为缺失右操作数、缺失 `if` 条件、缺失 `while` 条件、缺失 `throw` 操作数、空括号表达式等场景提供了错误恢复分支；如果没有 golden file 锁定这些路径，后续修改很容易让错误恢复退化、崩溃或悄悄改变输出而不被发现。

Kotlin 的参考实现也已经把这类场景纳入 `compiler/fir/raw-fir/psi2fir/testData/rawBuilder/expressions/`，并让 rawBuilder / lazyBodies 套件共同覆盖。当前仓库在目录组织与套件发现上都落后于这一基线，需要先通过提案明确补齐方向。

## 变更内容

- 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder` 下补齐表达式向的 testData 分类，新增一批“仓颉 parser 可恢复、builder 已有恢复路径”的缺失表达式样例，而不是继续把所有用例混在 `declarations/` 中。
- 对齐 Kotlin 的 rawBuilder 测试思路：保留主 rawBuilder suite 扫描 `testData/rawBuilder` 根目录的方式，并将当前仅覆盖 `testData/rawBuilder/declarations` 的 lazyBodies（by-ast / by-stub）套件调整为也能发现新增的 `expressions/` 目录。
- 为新增用例补齐 `*.txt` 与 `*.lazyBodies.txt` golden file；如果某些缺失表达式场景当前输出不稳定，则只做最小范围的 builder / renderer 修正，使 recoverable error output 能被稳定断言。
- 更新 README 与 OpenSpec 规范，记录 rawBuilder 对表达式错误恢复场景的覆盖要求与目录结构。

## 功能 (Capabilities)

### 新增功能

### 修改功能
- `raw-cfir-implementation`: Raw CFIR 的 rawBuilder 测试契约必须覆盖可恢复的缺失表达式场景，并保证新增表达式 testData 不会被 lazyBodies 套件漏掉。

## 影响

- 受影响模块：`cfir/raw-cfir/psi2cfir`（`testData`、`testFixtures`、`tests-gen`，必要时含 `PsiRawCfirBuilder` 的最小恢复输出修正）。
- 受影响代码：`TestGeneratorForPsi2Cfir.kt`、generated tests、`AbstractRawCfirBuilder*Test`、新增 `rawBuilder/expressions` testData 与对应 golden files。
- 受影响规范：`openspec/specs/raw-cfir-implementation/spec.md` 需要补入“缺失表达式回归覆盖”和“表达式目录套件发现”的要求。
- 受影响文档：`README.md` 需要同步记录该提案与 rawBuilder 测试覆盖缺口。
