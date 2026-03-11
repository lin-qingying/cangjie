## 上下文

当前仓库中，类体成员字段与其他位置的 `let`/`var` 声明已经在 PSI 层被明确区分为不同节点族：

- `CjAbstractClassBody.declarations` 通过 `CLASS_MEMBER_DECLARATION_TYPES` 暴露 `FIELD`，对应 `CjFieldVariable`；
- `CjFile.declarations` 通过 `FILE_DECLARATION_TYPES` 暴露 `VARIABLE`，对应 `CjPatternVariable`；
- `CjFieldVariable` 表示类成员字段，具有单一名称，但它在语义上不同于 `CjProperty`；
- `CjPatternVariable` 则可能表示 binding / tuple / enum / wildcard pattern，PSI 注释也明确说明它未必有单一名字。

与此相对，`PsiRawCfirBuilder.convertDeclaration()` 当前只处理 `CjProperty`，没有处理 `CjFieldVariable`。结果就是 `classWithMembers.cj`、`classWithTypeParameters.cj`、`structDeclaration.cj` 等类体字段样例都会落入 `else -> CfirProperty(name = <error-declaration>)` 的兜底分支。这里的问题有两层：一层是类成员字段漏掉了 `CjFieldVariable` dispatch；另一层是 `CjPatternVariable` 与现有 `CfirVariable(name)` 形状并不兼容。考虑到当前 `CfirVariable` / `CfirVariableSymbol` / renderer 都假定单一 `name`，最小改动的收敛方式不是重构整个变量层次，而是**保留现有 `CfirVariable` 不动**，继续让它表示具名单一变量，同时新增独立的 `CfirPatternVariable : CfirCallableDeclaration` 来承接模式变量声明，其中 `CfirPatternVariable` 持有完整 `CfirPattern`。

## 目标 / 非目标

**目标：**
- 让 rawBuilder 在 class/interface/struct/enum body 中正确处理 `CjFieldVariable`，不再输出 `<error-declaration>` 占位，并继续 lowering 到现有具名 `CfirVariable`。
- 锁定类体成员字段的顺序稳定性：字段位于函数前、函数后、构造器前后时，normal / lazyBodies 输出都必须稳定。
- 将“成员字段 vs 非成员 pattern variable”的作用域边界写入规范和设计，避免本次变更被误读为通用模式声明 lowering。
- 在实现阶段保留 `Field`、`Variable`、`Property` 的语义边界：具名单一变量继续使用现有 `CfirVariable`，模式变量使用新的 `CfirPatternVariable`，而不是用 `CfirProperty` 掩盖差异。`CfirPatternVariable` 需要保存完整 pattern，而不是只保存 binding 名称。

**非目标：**
- 不在本次变更中为文件级或局部 `CjPatternVariable` 提供通用 lowering。
- 不为 tuple / enum / wildcard / destructuring pattern 声明设计新的 Raw CFIR 表示。
- 不修改 parser 的 declaration 分类规则；本次只修复 rawBuilder 对既有 PSI 分类的覆盖。
- 不顺带重构 renderer、诊断框架或 resolve 阶段语义。

## 决策

### 决策 1：保留现有 `CfirVariable`，仅新增 `CfirPatternVariable : CfirCallableDeclaration`

**选择：**
- 保留当前具名单一变量的 `CfirVariable` 实现与 `CfirVariableSymbol` 不变；
- 新增 `CfirPatternVariable : CfirCallableDeclaration` 表示模式变量声明，并持有完整 `pattern: CfirPattern`；
- 在 `PsiRawCfirBuilder` 中为 `CjFieldVariable` 接入现有 `CfirVariable` lowering；
- 对 `CjPatternVariable` 的 future lowering 单独对接到 `CfirPatternVariable`。

**原因：**
- 代码注释已经明确：`CjFieldVariable` 是“类成员字段”，`CjProperty` 是 property 语义，`CfirProperty` 对应 `PropDecl`，`CfirVariable` 对应局部 `VarDecl`；
- 当前 `CfirVariable`、`CfirVariableSymbol` 和 renderer 都深度绑定单一 `name`；如果为了 pattern variable 去重构整条变量层次，改动面会明显扩大；
- `CjPatternVariable` 自身拥有完整 pattern，且一个声明可能派生 0..N 个 binding；`CfirPattern` 已经存在，新增 `CfirPatternVariable(pattern)` 能自然承接这部分结构；
- 这样既避免把字段错误落成 `Property`，也避免为了引入模式变量而破坏现有具名变量路径。

**备选方案：**
- 方案 A：将 `CfirVariable` 抽象化，并新增 `CfirFieldVariable` / `CfirPatternVariable`。拒绝原因：虽然概念上整齐，但当前仓库里 `CfirVariable` 的 symbol、renderer、visitor 都按单一名字工作，重构成本比新增一个独立 pattern node 更高。
- 方案 B：继续将成员字段 lowering 为 `CfirProperty`。拒绝原因：`Property` 与 `Field` 不是同一概念。

### 决策 2：本次需求只覆盖类体中的简单命名成员字段

**选择：**
- 规范中明确限定“完整行为修复”先落在 class-like body 中的 `CjFieldVariable`；
- 同时，在 CFIR 层先把 `CfirPatternVariable` 节点建出来，为文件级/局部 `CjPatternVariable` 预留正确落点；
- 对文件级/局部 `CjPatternVariable` 的完整 lowering 行为仍保持收敛，不在本提案里承诺一次性补全所有 pattern 形态；
- raw CFIR 只需要保留完整 pattern 结构，不需要把 pattern 导出的 0..N 个 binding 作为持久子变量节点重复存储。

**原因：**
- `CjPatternVariable` 可能没有单一名字，因此 `CfirPatternVariable` 必须挂接完整 `CfirPattern`，而不能简单复刻旧 `CfirVariable(name)`；
- PSI 的 `getAllBindings()` 与 `getAllPatternDeclarations()` 已经说明“绑定列表/声明列表”应该是从 pattern 派生的查询，而不是 variable declaration 自己重复维护的存储字段；
- 现有类成员字段问题并不要求重写具名变量语义，只需要让 `CjFieldVariable` 不再掉进 `CfirProperty(<error-declaration>)` 兜底；
- 如果把本次提案写成“修复不同位置的 let 声明”，很容易误承诺 tuple / enum / wildcard pattern 的 Raw CFIR 支持；
- 用户当前实际举例是 `classWithMembers.cj`，其核心歧义正是“成员普通变量”与“别的位置模式变量”要分开看待。

**备选方案：**
- 方案 A：同时修复所有简单 binding 形式的 `CjPatternVariable`。拒绝原因：虽然理论上某些 simple binding 可以降到单名变量，但当前提案阶段没有足够证据表明所有 file/local case 都安全，范围会失控。
- 方案 B：把每个 binding 都直接落成独立 `CfirVariable` 节点。拒绝原因：会丢失 tuple/enum/wildcard/type pattern 的原始结构，并制造 pattern 与 bindings 的双重真相。

### 决策 3：增加位置敏感回归测试，而不是只更新现有 golden

**选择：**
- 更新 `classWithMembers`、`classWithTypeParameters`、`structDeclaration` 等现有 golden；
- 额外新增至少一个“字段不在类体开头”的用例，覆盖字段出现在函数/构造器前后或成员交错顺序中的场景；
- normal rawBuilder 与 lazyBodies 都共享这一组位置敏感基线要求。

**原因：**
- 当前 `classWithMembers` 只覆盖“字段先于函数和 init”的顺序，还不能证明位置敏感分发已经被锁定；
- 用户明确提到了“let 声明在不同位置的情况”，仅更新一个已有文件无法覆盖这个真实意图；
- 位置敏感测试能防止未来再因为 class body 遍历或 declaration dispatch 顺序改变而回归。

**备选方案：**
- 方案 A：只更新现有 `classWithMembers` / `classWithTypeParameters` golden。拒绝原因：无法验证字段出现在其他成员顺序中的稳定性。
- 方案 B：把所有 class-body declaration 场景都重写为大型综合用例。拒绝原因：基线过大、不利于定位失败。

### 决策 4：README 与规范同时记录“支持什么 / 不支持什么”

**选择：**
- 在 OpenSpec `raw-cfir-implementation` 中新增成员字段 lowering 需求；
- 在 `README.md` 的 OpenSpec 变更进展中同步记录该提案，并注明其非目标是通用 pattern variable lowering。

**原因：**
- 这类问题很容易被误解为 parser、renderer 或“所有 let 声明”问题；
- 将边界写进文档，可以减少后续实现或归档时的 scope 漂移。

**备选方案：**
- 方案 A：只在任务列表里说明边界。拒绝原因：任务容易过期，规范和 README 才是长期约束。

## 风险 / 权衡

- [风险] 现有 top-level / local `CjPatternVariable` golden 仍然可能继续输出 `<error-declaration>`，用户可能误以为“这次修复不完整”。→ **缓解**：在 proposal/design/spec 中明确写出非目标，并强调 `CfirPatternVariable` 先保结构、后补完整 lowering。
- [风险] 新增 `CfirPatternVariable` 后，visitor / renderer / symbol 需要多一个 callable declaration 分支。→ **缓解**：保持现有 `CfirVariable` 路径完全不变，只为 pattern variable 增量增加 `visitPatternVariable` 与独立 symbol。
- [风险] `CfirPatternVariable` 如果同时保存 `pattern` 和“展开后的 binding 变量列表”，容易产生双重真相。→ **缓解**：规范明确 binding 列表应为 derived query，由 pattern 遍历得到，而非持久字段。
- [风险] 某些类体成员还可能使用 `CjProperty` 而不是 `CjFieldVariable`，如果实现时处理不当可能造成双路径不一致。→ **缓解**：保留 `CjProperty` 现有分支，并把字段分支作为独立路径处理。
- [风险] 新增位置敏感测试后可能暴露更多历史 class-body lowering 缺口。→ **缓解**：先以少量精确 case 建立基线，必要时再拆分后续提案。

## 迁移计划

1. 先在 `openspec/changes/fix-rawbuilder-let-position-handling/` 中补齐 proposal / design / spec / tasks，固定作用域。
2. 实现阶段先新增 `CfirPatternVariable : CfirCallableDeclaration`，同步补齐对应的 visitor / renderer / symbol 层。
3. 再修改 `PsiRawCfirBuilder` 的 declaration dispatch，让 `CjFieldVariable` 走现有 `CfirVariable` lowering；对 `CjPatternVariable` 先建立 `pattern` 驱动的结构表示，并保留后续分步接入空间。
4. 更新 `classWithMembers`、`classWithTypeParameters`、`structDeclaration` 等 golden，并新增至少一个字段顺序回归用例；必要时重新生成 tests-gen。
5. 运行 `:cfir:raw-cfir:psi2cfir:test` 验证 normal + lazyBodies 覆盖通过。
6. 更新 `README.md` 与 OpenSpec 规范，记录能力与非目标范围。

回滚策略：
- 若实现阶段发现 renderer/visitor 仍然假设所有 callable declaration 都按名称显示，优先给 `CfirPatternVariable` 增加独立 visit/render 入口，而不是挤进现有 `visitVariable(name)` 路径；
- 若只是新增位置测试暴露出额外历史差异，可先最小化到更新现有 class-member golden，再用后续变更扩充顺序覆盖。

## 开放问题

- class-like body 中是否还存在需要视为成员字段但不走 `CjFieldVariable` 的边缘 PSI 形态，需要在实现阶段用实际测试确认。
- `CfirPatternVariable` 的 derived API 应该暴露哪些查询：仅 `bindings`，还是同时提供“all pattern declarations”，需要在实现前与 resolve 使用方一起拍板。
- 文件级 / 局部的 simple binding `CjPatternVariable` 是否值得后续单独提案为“single-binding subset lowering”，还是应等待 pattern-aware CFIR 设计统一处理。
