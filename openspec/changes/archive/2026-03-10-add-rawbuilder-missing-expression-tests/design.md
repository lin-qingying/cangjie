## 上下文

本仓库当前的 rawBuilder 测试布局与 Kotlin 参考实现存在两个直接缺口。

第一，`cfir/raw-cfir/psi2cfir/testData/rawBuilder` 目前只有 `declarations/` 子目录，现有 `.cj` 样例全部是完整源码；即使 `controlFlow.cj`、`functionExpressions.cj` 含有表达式，也没有任何专门针对“缺失表达式 / 不完整表达式”的输入。直接 grep 本地 testData，也找不到 `missing`、`incomplete` 或等价的 recoverable malformed case。

第二，测试生成器的覆盖范围与 Kotlin 不一致。当前 `TestGeneratorForPsi2Cfir.kt` 将主 rawBuilder suite 指向 `testData/rawBuilder`，但两个 lazyBodies suite 只指向 `testData/rawBuilder/declarations`。这意味着即便后续新增 `expressions/` 目录，主 suite 能看到，lazyBodies by-ast / by-stub 仍会漏掉。相比之下，Kotlin 的 `TestGeneratorForPsi2Fir` 三个 rawBuilder suite 都扫描整个 `rawBuilder/` 根目录，而其 `testData/rawBuilder/expressions/` 已包含 `cascadeIf.kt`、`simpleReturns.kt`、`in.kt` 等表达式样例，其中 `cascadeIf.kt` 明确覆盖了缺失子表达式 / 不完整分支场景。

同时，本地 `PsiRawCfirBuilder` 已经具备一批可测试的恢复路径：
- `convertBinary()` 对缺失左/右操作数返回 `buildErrorExpression("Missing ... operand")`
- `convertIf()` 对缺失 condition 返回错误表达式，缺失 then/else 分支则回落为空 block / null
- `convertWhile()` / `convertDoWhile()` / `convertThrow()` 对缺失必需子表达式生成错误表达式
- `CjParenthesizedExpression` 在缺失内部表达式时也会生成错误表达式

`CjIfExpression` 的 PSI 访问器本身也把 `condition`、`then`、`else` 暴露为可空子节点，这说明 PSI API 能表示部分解析的 `if`。具体哪些 malformed 源码形态会被当前 parser 恢复并进入 rawBuilder，仍需在实现阶段以实际 parse/build 结果为准。换言之，本次缺的不是能力方向，而是面向这些入口的回归测试与目录组织。

## 目标 / 非目标

**目标：**
- 为 rawBuilder 增加一个清晰的表达式测试分类，专门覆盖缺失表达式与错误恢复路径。
- 锁定一批“仓颉 parser 可恢复、rawBuilder 已有恢复分支”的输入，使其具备稳定的 `*.txt` / `*.lazyBodies.txt` 基线。
- 消除“主 rawBuilder 能看到新目录、lazyBodies 看不到”的结构性漏测问题。
- 让 Kotlin 的 `rawBuilder/expressions` 组织方式成为参考基线，但只迁移到与仓颉语法和当前 builder 能力相匹配的最小集合，而不是追求逐文件对齐。

**非目标：**
- 不在本变更中照搬 Kotlin 的全部表达式样例，也不承诺与 Kotlin 的 textual dump 一字不差。
- 不扩展 parser 的错误恢复算法；仅使用当前 parser 能恢复并进入 PSI/CFIR 的场景。
- 不在本变更中引入新的诊断框架、独立的 stub-only 基线体系，或与缺失表达式无关的大规模 renderer 重构。

## 决策

### 决策 1：新增 `rawBuilder/expressions/` 目录，而不是继续堆在 `declarations/`

**选择：**
- 在 `cfir/raw-cfir/psi2cfir/testData/rawBuilder/` 下新增 `expressions/` 目录；
- declaration-focused case 继续留在 `declarations/`，recoverable missing-expression case 统一放入 `expressions/`。

**原因：**
- 当前 `declarations/` 已经混合了承载声明结构与少量正常表达式的样例，再继续塞入错误恢复场景会让目录语义进一步模糊；
- Kotlin 参考实现已经将这类样例单独归档到 `rawBuilder/expressions/`，这一组织方式更利于查找、增量扩充与对照分析。

**备选方案：**
- 方案 A：继续把新样例放在 `declarations/`，贴近 `controlFlow.cj` / `functionExpressions.cj`。拒绝原因：目录语义不清晰，也无法一眼看出当前仓库对 expression recovery 的真实覆盖面。
- 方案 B：新建 `errors/` 或 `recovery/` 目录。拒绝原因：会偏离 Kotlin 的参考布局，而且这些场景本质仍是表达式构建，不是独立测试体系。

### 决策 2：只覆盖“parser 可恢复 + builder 已有恢复分支”的缺失表达式

**选择：**
- 首批用例只选取能稳定进入现有 builder 分支的场景，例如缺失二元右操作数、空括号表达式、缺失 `if` 条件、缺失 `while` 条件、缺失 `throw` 操作数、以及其他能形成 nullable PSI 子节点的表达式；
- 以 Kotlin 的 `cascadeIf.kt`、`simpleReturns.kt` 等为参考，翻译成仓颉语法允许且本地 parser 能恢复的对应形式。

**原因：**
- 本仓库和 Kotlin 的语法并不相同，直接逐文件照搬会制造大量“根本进不了 parser/builder”的伪需求；
- 本地代码已给出明确信号：很多错误恢复输出已经存在，只是没有被 testData 锁定。

**备选方案：**
- 方案 A：逐字复刻 Kotlin 的 `expressions/` 样例。拒绝原因：仓颉语法与 Kotlin 差异大，很多用例既不自然也不一定可解析。
- 方案 B：只测最简单的二元表达式缺右操作数。拒绝原因：无法覆盖 control-flow recovery、空 block / null branch 等 builder 现有逻辑，收益太低。

### 决策 3：lazyBodies suite 必须跟随 `rawBuilder/` 根目录，而不是继续绑死 `declarations/`

**选择：**
- 将 `RawCfirBuilderLazyBodiesByAstTestGenerated` 与 `RawCfirBuilderLazyBodiesByStubTestGenerated` 的输入范围对齐到 `testData/rawBuilder` 根目录，使其和主 rawBuilder suite 一样递归发现 `declarations/` 与 `expressions/`；
- 继续沿用当前仓库的 `.lazyBodies.txt` 期望文件策略，除非新增样例暴露出 by-stub 需要单独基线的证据。

**原因：**
- 这是当前仓库与 Kotlin 参考实现的最大结构差异；如果不修，新增 `expressions/` 只会让 normal suite 有覆盖，lazyBodies 继续漏测；
- 仓库现有 generated tests 和 README 都强调 all-files-present 等效校验，但这个护栏现在只对已纳入 suite 的目录有效。

**备选方案：**
- 方案 A：只让主 rawBuilder suite 覆盖 `expressions/`。拒绝原因：会把“表达式恢复在 lazy mode 是否稳定”留成盲区。
- 方案 B：为 `expressions/` 单独再造一组 lazyBodies suite。拒绝原因：复杂度更高，而现有生成器已经支持递归目录发现，没有必要重复造轮子。

### 决策 4：若新增样例暴露不稳定输出，只做最小 builder / renderer 收敛

**选择：**
- 将本变更定义为“测试驱动的覆盖补齐”；
- 如果某个 recoverable case 当前输出不稳定、空洞或无法被 golden 明确表达，则只允许做最小范围的 `PsiRawCfirBuilder` / `CfirRenderer` 修正，使其稳定落到 `ERROR_EXPR(...)`、空 block、null result 等现有约定中。

**原因：**
- 用户当前要解决的是缺失表达式测试，而不是新的错误语义系统；
- 本地 renderer 已能输出 `ERROR_EXPR(reason)`，builder 也已在多处分支调用 `buildErrorExpression(...)`，因此应该优先利用现有约定，而不是借机扩 scope。

**备选方案：**
- 方案 A：仅新增 testData，不允许任何代码修正。拒绝原因：如果现有输出对 recoverable case 不稳定，最终仍然没法形成可靠基线。
- 方案 B：顺手设计一套全新的错误表达式渲染协议。拒绝原因：与本任务不成比例，且已被其他 renderer 架构工作覆盖。

## 风险 / 权衡

- [风险] 仓颉 parser 对“不完整表达式”的恢复能力弱于 Kotlin，导致部分参考场景无法进入 rawBuilder。→ **缓解**：实现时先用当前 PSI nullable 子节点与 builder 分支做白名单，按“能恢复的先测”策略推进。
- [风险] 把 lazyBodies 根目录扩大到 `rawBuilder/` 后，会立刻暴露历史未覆盖路径与 golden 缺口。→ **缓解**：先新增少量表达式样例并同步更新 generated tests / baselines，避免一次性扩得过大。
- [权衡] 继续复用 `.lazyBodies.txt` 作为 by-ast / by-stub 的共用基线，能保持当前仓库简单性；代价是无法像 Kotlin 那样天然区分 stub-specific 差异。→ **缓解**：若实现时观测到稳定差异，再单独提案扩展基线体系。
- [风险] 某些恢复场景可能落成空 block/null 而不是 `ERROR_EXPR(...)`，若提案写死输出形式，会造成无谓返工。→ **缓解**：规范只要求“稳定、可断言的恢复输出”，允许是错误表达式或现有约定的空占位。

## 迁移计划

1. 在 `rawBuilder/` 下引入 `expressions/` 目录，并先挑选 3~5 个仓颉侧可恢复的缺失表达式样例。
2. 调整 `TestGeneratorForPsi2Cfir.kt`，让 lazyBodies suite 与主 suite 对齐到 `rawBuilder/` 根目录；重新生成 tests-gen。
3. 为新增样例产出 `*.txt` / `*.lazyBodies.txt`，必要时对 builder / renderer 做最小稳定化修正。
4. 运行 `:cfir:raw-cfir:psi2cfir:test` 验证 normal + lazyBodies 全部通过，并更新 README / OpenSpec 规范。

回滚策略：
- 若扩大 lazyBodies 覆盖面后暴露过多历史差异，可先保留 `expressions/` 主 suite 覆盖与对应 design/spec，暂时回滚生成器路径修改，再用后续小步变更继续推进 lazyBodies 对齐。

## 开放问题

- 仓颉语法中哪些“缺失表达式”形态最稳定地进入当前 parser 恢复路径，适合作为首批长期基线？
- bare `return` 之类 Kotlin 参考场景在仓颉里是否自然、是否需要替换成更贴近本语言的等价形式？
- by-stub lazyBodies 后续是否需要 Kotlin 式的独立 baseline（如 `*.stub...txt`），还是继续共用 `.lazyBodies.txt` 即可？
