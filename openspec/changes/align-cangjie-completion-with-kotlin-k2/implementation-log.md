# 实施与验证记录

## 工作副本（2026-10-03）

- 宿主：本工作树 `claude/musing-bun-5ae793`，初始 HEAD `48c416ddf6a12162dc54422ea00f04106ba9810f`。
- 上游：`D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun`，分支 `claude/completion-k2-musing-bun`，初始 HEAD `b10638242d4ad36878350c4e58154542a45e19f7`。
- 主检出上的 7 个已修改文件未迁移、未覆盖；实现不写入主检出。
- 原计划、源码指纹保持为实施前证据；本记录用于跟踪新的执行结果。未运行项目不得标记已验证。

## 1.1 源码基线复核

2026-10-03 使用 Python `pathlib`/`hashlib.sha256` 逐项读取 `source-baseline.json` 的 85 个条目，按 `roots` 映射核对实际内容。结果：**85/85 文件存在且 SHA-256 一致，0 项漂移**。主检出及宿主 HEAD 均与设计相同。两份 Kotlin 参考包含在 85 项指纹检查中。此结果仅确认设计依据未漂移，不证明功能已通过测试。

## 1.2 上游根路径配置

修改 `settings.gradle.kts`：

- 优先读取 `cangjie.upstream.root` Gradle 属性，其次 `CANGJIE_UPSTREAM_ROOT` 环境变量，默认 `..`。
- 相对路径以宿主 settings 目录解析；校验选中上游的 `settings.gradle.kts` 存在。
- 复合构建和本地 `build/repo` 使用同一上游根，不嵌入本机路径。
- 同步 `docs/architecture-host-plugin.md` 的配置契约。

已执行（宿主工作树）：

```bash
./gradlew help -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun --offline --console=plain
```

结果：**BUILD SUCCESSFUL**（1m 38s，19 tasks executed），确认绝对路径可配置。日志：会话后台任务 `bz8858yoj`。该命令只验证 settings/build logic 配置，不是 Analysis 或 completion 编译测试。

追加验证（2026-10-03，日志任务 `btpalruap`）：

```bash
CANGJIE_UPSTREAM_ROOT=../../../../.claude/worktrees/completion-k2-musing-bun ./gradlew help --offline --console=plain
```

结果：**BUILD SUCCESSFUL**（4s），证明不依赖绝对开发机路径。

```bash
./gradlew help -Pcangjie.upstream.root=./missing-upstream-for-validation --offline --console=plain
```

结果：按预期 **BUILD FAILED**（1s），错误为 `Cangjie upstream settings.gradle.kts not found ... Set -Pcangjie.upstream.root=<upstream-checkout> or CANGJIE_UPSTREAM_ROOT.`，未继续进入下游构建。

## 1.3 / 1.5 消费矩阵

见 [消费矩阵](consumer-matrix.md)：25 个固定 contributor、chain 与 command-completion 边界、旧 API 调用者与兼容策略、以及本机实测的 SDK 制品归属。

## 4.3 / 4.4 签名连续替换与变量种类

改动：

- 新增 `cfir/cfir-cones/.../substitution/CfirChainedSubstitutor.kt`：顺序应用两次替换，对齐 Kotlin `ChainedSubstitutor`。
- `CaCfirSubstitutedSignatureModel.kt`：函数与变量签名不再对二次非空替换抛错；改为链式替换，并按 substitutor 参与 `equals`/`hashCode`（此前替换前后签名恒等，无法区分不同实例化）。
- `CaCfirSignatureModel.kt`：`renderVariableSignature` 放宽为 `CfirCallableSymbol`，property 不再因强转 `CfirVariableSymbol` 失败。

执行（上游工作树）：

```bash
./gradlew :analysis:analysis-api-cfir:test --tests '*AnalysisApiSignatureExecutionTest*' --offline --console=plain
```

结果：**PASS**（`chainedFunctionAndVariableSignatures`）。覆盖顶层泛型函数 `T -> U -> User` 顺序替换、反序不合并、prop/field/局部变量三类替换、空替换返回自身、声明身份与 PSI 一致。

夹具 `analysis/analysis-api-cfir/testData/signatures/chainedFunctionAndVariableSignatures.cj` 已用官方 `cjc 1.1.3`（`cangjie-sdk-windows-x64-1.1.3/cangjie/envsetup.sh`）验证 `--output-type=staticlib` 成功，仅有 unused-variable 警告。

## 字段成员缺失缺陷：已修复并加回归锁定

上一轮定位到：`CfirClassDeclaredMemberScope`、`CfirExtendMemberScope`、`CfirClassSubstitutionScope` 都只把变量放进索引并经 `processCallablesByName` 透出，没有覆写 `processVariablesByName`；而 Analysis 的 `getCallableSymbols` 走的正是后者。三处补齐后症状仍在，继续沿链下探找到真正的包装层：**`CfirClassUseSiteMemberScope`** —— `CaType.scope` 实际落到的就是它，同样只覆写了 callable、属性与名称集合。补上其 `processVariablesByName`（复用 `processCallablesByName` 的可见集合，保证两者完全一致）后，字段候选出现。

补齐字段后共享 `memberScope` 套件立刻暴露第二个缺口：**`CaCfirFieldSymbol.createPointer` 直接抛 `Field symbol cannot create a stable pointer`**，成员作用域一旦真的暴露字段就无法被渲染。已按 `CaCfirMemberFunctionSymbolPointer` 的形状实现 `CaCfirMemberFieldSymbolPointer`（owner 指针 + 字段短名，从 owner 的声明成员作用域按名恢复；找不到返回 `null` 不伪造符号），并在字段符号侧改为真实创建指针——缺 owner 类标识或 owner 不是声明容器时如实报错。

回归锁定：共享夹具 `analysis/analysis-api/testData/components/scopeProvider/memberScope/memberScopeQueries.cj` 此前**只含函数**，从未覆盖字段。现补入 `public var field` 与 `public let state`，并为声明成员作用域加入 `field`/`state` 的可用名与按名查询指令。golden 用框架自带的 `-Dupdate.test.data=true` 重生成，`MemberScope` 渲染结果现在包含 `CaCfirFieldSymbol` 条目。use-site member scope 的 `field`/`state` 指令暂未加入，原因记录在夹具注释中。

```bash
./gradlew :analysis:analysis-api-cfir:test --tests '*MemberScopeTestGenerated*' \
  --tests '*DeclaredMemberScopeTestGenerated*' -Dupdate.test.data=true --offline --console=plain
./gradlew :analysis:analysis-api-cfir:test --tests '*ScopeTestGenerated*' \
  --tests '*MemberScopeTestGenerated*' --tests '*DeclaredMemberScopeTestGenerated*' \
  --tests '*CombinedDeclaredMemberScopeTestGenerated*' --tests '*TypeScopeTestGenerated*' \
  --tests '*AnalysisApiCfirComponentExecutionTest*' \
  --tests '*CfirLocalScopeCompletionSectionTest*' --tests '*CfirMemberCompletionSectionTest*' \
  --tests '*CfirTopLevelCompletionSectionTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL，79 tests / 0 failures**。字段成员缺陷修复并被共享套件锁定；`CfirMemberCompletionSectionTest.memberCandidates` 随之转绿。

字段指针的补齐同时解除了 10.2 的一个硬前置：候选快照要「分析结束后按指针重新进入分析」，此前字段成员根本没有可用指针。

## 9.2 成员与 extend 候选区段（通过）

`impl-cfir` 新增 `CfirMemberCompletionSection`（优先级 20）：候选来自 Analysis 的类型作用域与 extend 提供者；每个候选先过 use-site 可见性检查，extend 成员先过适用性检查。`Unknown` 一律视为「可能适用/可能可见」——把未判定项一起丢弃会让补全缺失合法候选。

```bash
./gradlew :code-insight:completion:impl-cfir:test --tests '*CfirMemberCompletionSectionTest*' --offline --console=plain
```

结果：**3/4 PASS**（可见成员出现、private 过滤、前缀过滤、非成员访问位置不产出成员候选通过；`memberCandidatesIncludeExtend` 仍失败，只看到 `[MEMBER]`，没有任何 `EXTEND_MEMBER`）。

> 更正：本节此前记录过「4/4 PASS」，那是**错误结论**。当时的命令行同时带了两个 `--tests` 过滤，其中成员区段用例未真正执行，Gradle 因另一个过滤命中而整体成功。此后单独运行确认 extend 用例仍失败。9.2 已撤回勾选。教训：一次运行里混用多个 `--tests` 时必须核对 XML 中实际执行了多少用例，不能只看 BUILD SUCCESSFUL。

过程中修掉一处已确认缺陷（另一处未解决）：

1. **字段成员缺失**：见上一节，根因是四层作用域都没覆写 `processVariablesByName`，实际落点是 `CfirClassUseSiteMemberScope`。连带补齐字段稳定指针。
2. **extend 成员被适用性检查全部过滤（仍未解决）**：适用性检查此前通过 `extendSymbol.psi as? CjExtend` 反查 PSI 再推进阶段，而区段用的是 `getExtendSymbols(classId)` 的 **provider 派生** extend 符号——这类符号可能没有 PSI，推进无效，`extendedTypeRef.coneTypeOrNull` 恒为 `null`，判定退化为 `ReceiverMismatch`/`Unknown`。已改为直接对 CFIR 元素调用 `lazyResolveToPhase(CfirResolvePhase.TYPES)`（5.1/5.2 用例通过），但区段侧 extend 成员**仍未出现**，说明还有第二处原因：可能是 `getExtendSymbols` 对同文件 extend 返回空、适用性结论仍为否定、或 extend 成员的作用域过滤过严。未继续定位。

Analysis 侧合并窄集：**BUILD SUCCESSFUL，269 tests / 0 failures**（作用域上下文、可见性、extend 适用性、期望类型与其生成套件、签名、副本分析、dangling 生成用例、文件/声明/组合/类型作用域套件、既有候选三态执行测试、三个补全区段测试）。

## 9.5 关键字与声明名候选区段（部分通过）

`impl-cfir` 新增 `CfirKeywordCompletionSection`（优先级 5，最先执行）：关键字是语言语法事实、声明名是包内已有符号的拼写，两者都**不从作用域取**——从作用域取会让关键字永远缺席或让声明名脱离可见性。

设计上的关键约束已落到代码：`this` 只在位置上下文确实存在隐式接收者时提供，缺失即不提供。

```bash
./gradlew :code-insight:completion:impl-cfir:test --tests '*CfirKeywordCompletionSectionTest*' --offline --console=plain
```

结果：**4/4 PASS**（已核对 XML 中实际执行 4 个用例）：函数体位置给 `return`/`let`、成员访问位置给 `this`、**无接收者位置不给 `this`**（缺席断言）、声明名位置给 `func` 且只给关键字、任何位置都不出现 Kotlin 专属关键字 `suspend`/`companion`/`by`/`out`/`reified` 等。

过程中发现并规避一个真实递归缺陷：最初在**声明名位置**也去取导入作用域以建议类型名，实测触发 `StackOverflowError`——在声明标识符上取导入作用域会与正在解析的声明互相递归。已把类型名建议限制在类型位置，并在代码注释与测试 KDoc 中写明该边界。

因此 9.5 **不勾选**：关键字侧完整，声明名/类型建议侧只覆盖类型位置。

## 9.3 顶层与导入候选区段（通过）

`impl-cfir` 新增 `CfirTopLevelCompletionSection`（优先级 30），候选来自 Analysis 的**导入作用域上下文**：该上下文已按编译器顺序组合本文件顶层声明、同包成员、显式导入（含别名）与语言默认导入。按 PSI 声明列表自行拼候选会丢掉导入别名与来源优先级，也无法区分「已导入」与「需要导入」。已导入候选权重高于需导入候选——导入成本是排序的一部分。

测试 `CfirTopLevelCompletionSectionTest`（4 个用例 + 4 个编辑态夹具，夹具的未解析标识符经 cjc 确认为语义错误而非语法错误）：

```bash
./gradlew :code-insight:completion:impl-cfir:test --tests '*CompletionSectionTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL（8/8，含 9.1 的 4 个）**。覆盖顶层声明可见、前缀过滤、同名重载保留、非成员访问位置前提成立。

过程中修掉的一个真实缺陷：顶层 callable 候选最初不渲染签名，`tailText` 为空串，去重键退化成「显示名」，**同名重载被合并成一个候选**——正是设计禁止的「按 label 去重」。已改为在分析内渲染公开签名视图作为尾文本，签名同时承担身份区分。

## 9.1 局部作用域候选区段（部分通过，缺口已定位）

`impl-cfir` 新增 `CfirLocalScopeCompletionSection`（优先级 10）与 `CfirCompletionService`：候选来自 Analysis 的位置作用域上下文而非 PSI 祖先遍历；签名渲染在**分析内**完成，快照离开会话后不持有符号或类型。成员访问与 import 位置不使用词法候选。

测试 `CfirLocalScopeCompletionSectionTest`（4 个用例 + 4 个夹具，`code-insight/completion/impl-cfir/testData/sections`，夹具经官方 cjc 1.1.3 编译通过）：

```bash
./gradlew :code-insight:completion:impl-cfir:test --tests '*CfirLocalScopeCompletionSectionTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL（4/4）**，但其中两条断言固定的是**已知缺口**而非目标行为：

- 函数参数 `param` 可见、前缀过滤正确、快照脱离会话后仍可重复读取、另一函数内的局部不泄漏 —— 这四条是**已验证通过**的。
- **块内 `let` 局部 `local` 仍不可见**，测试显式断言其不可见并注明底层修复后应改为断言可见。这与 3.x 记录的「块内局部声明未进入解析塔快照」是同一个 LL 缺陷，在补全区段上表现为局部候选缺失。

因此 9.1 **不勾选**：目标行为（局部声明可见）尚未成立。

过程中修掉的一个真实缺陷：区段最初用 `scope.callables(emptySet())` 枚举候选，**空名称集合只会返回空结果**，导致所有局部候选消失；已改为 `callables { true }` 名称过滤器枚举。

## 8.4 dummy 修正（实现完成，产品级验证仍被平台夹具阻塞）

`impl-shared` 新增 `CaCompletionDummyIdentifierProvider`：平台已把 `IntellijIdeaRulezzz` 插进**文件副本**，但这串文本在仓颉语法里通常不是合法表达式（未完成调用 `toDummy`、未闭合实参列表），会让解析与作用域解析失败——表现为**候选为空且不报任何错**。实现按位置形态把光标处表达式替换为语法合法的占位表达式（调用类位置用可调用占位符，类型/声明名等位置保持裸标识符以免改变候选形态），并排除原始字符串（不插值）。只做仓颉需要的形态，未复制 Kotlin 的 KDoc、`$name` 或尾随 lambda 规则。

平台入口 `CaCangJieCompletionContributor.fillCompletionVariants` 已改为**先做 dummy 修正再调用服务**，请求的位置、前缀与替换区间全部来自修正后的副本；`offset` 单独保留编辑器偏移并在 KDoc 说明与 PSI 偏移不可混用。

```bash
./gradlew :code-insight:completion:impl-shared:compileKotlin --offline --console=plain   # BUILD SUCCESSFUL
```

产品级验证（`CangJieCompletionRegistrationTest`）当前**仍失败**，但失败原因已从「候选为空」变成平台夹具层问题：

```
Invalid PSI Element: LeafPsiElement because: different providers:
com.intellij.psi.DummyHolderViewProvider{ vFile=LightVirtualFile: \DummyHolder ... }
```

平台把 `DummyHolder` 而非真实仓颉文件交给了 contributor，说明该轻量夹具下 `CompletionParameters` 未绑定到 `CjFile`。这是 14.2 的真实端到端验收要解决的问题（真实产品中由 IDE 编辑器提供绑定文件），当前未定位到根因，**不猜测修复**。

因此 8.4 与 12.5 仍不勾选：实现已完成，但端到端候选产出未被证明。

## 12.5 产品级补全接线验证（部分通过，候选产出被 8.4 阻塞）

新增 `CfirCompletionServiceProvider`（项目级服务，首次访问时装入注册表）：上游实现是否随插件分发成为唯一装配开关，宿主不需要知道任何内部区段。产品测试 `CangJieCompletionRegistrationTest`（宿主 `product/idea-plugin`，3 个用例）。

```bash
./gradlew :product:idea-plugin:test --tests '*CangJieCompletionRegistrationTest*' \
  -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun \
  --offline --console=plain
```

结果：**3 个用例中 1 PASS / 2 FAIL**。

- **PASS** `testCompletionServiceIsAvailableInProduct`：产品环境能取到上游补全服务，证明描述符注册、服务提供者与制品装配在真实产品 classpath 中成立。
- **FAIL** 两个候选用例：平台补全不再报 PSI 错误，但产出候选为**空列表**。

过程中修掉三个真实缺陷：

1. **模块命名冲突**：`:code-insight:completion:api` 与既有 `:code-insight:api` **叶子名相同**，Gradle 在复合构建依赖图中把两者折叠为同一项目（`project ':code-insight:completion:api' -> project ':code-insight:api'`），导致宿主测试编译期看不到 `CaCangJieCompletionService`。已重命名为 `:code-insight:completion:contracts` 并同步 settings、module-catalog、README、两种制品与两个实现模块的依赖。
2. **dummy 位置是叶子错误元素**：平台插入 dummy 标识符后 `parameters.position` 是 `LeafPsiElement`，直接强转 `CjElement` 会触发平台 PSI 断言。已改为向上取最近的仓颉元素作为分析锚点。
3. **PSI 偏移与文档偏移混用**：轻量夹具中二者属于不同坐标系，混用产生 `Range [94, 21) out of bounds`。已把替换区间与前缀截取统一到 PSI 偏移，`offset` 保留编辑器偏移并在 KDoc 说明两者不可混用。

**剩余阻塞**：候选为空是 **8.4（dummy 标识符与位置修正）尚未实现**的直接后果——平台插入的 dummy 标识符仍在分析位置里，作用域上下文因此拿不到局部候选。在 dummy 修正落地前，端到端候选产出不能声称通过，因此 12.5 与 8.4 均不勾选。

## 8.7 生产 XML 接线（接线与宿主编译已验证，运行期触发未验证）

上游新增生产描述符 `code-insight/completion/impl-cfir/resources/META-INF/code-insight/cangjie-code-insight-completion.xml`，只注册**单职责入口** `CangJieCfirCompletionContributor`（继承平台 `CompletionContributor`，从项目级 `CaCangJieCompletionServiceRegistry` 取服务）。内部区段由服务显式列表组织，没有为每个区段增加扩展点。

宿主 `modules/ide/base/src/main/resources/org.cangnova.cangjie.ide.base.xml` 增加一行 `xi:include`。该描述符同时被产品 `plugin.xml` 与测试宿主聚合，因此接线对产品与测试环境一致生效。

```bash
./gradlew :code-insight:completion:impl-cfir:compileKotlin \
          :code-insight:completion:impl-cfir:processResources --offline --console=plain   # BUILD SUCCESSFUL
./gradlew :modules:ide:base:compileKotlin \
          -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun \
          --offline --console=plain                                                       # BUILD SUCCESSFUL (11m13s)
```

宿主编译结果证明：制品坐标、substitution、compileOnly/runtimeOnly 依赖与描述符 include 路径**在真实复合构建中成立**（225 tasks executed）。

运行期触发与候选产出见上方 12.5：服务装配已验证通过，候选产出被 8.4 的 dummy 修正阻塞。

## 8.5 补全位置分类（通过）

`impl-shared` 新增 `CaCompletionPositionContext` / `CaCompletionPositionKind` / `CaCompletionPositionContexts`：只判定位置**形态**，不产出候选、不做可见性判断。仓颉特有形态（命名实参、CDoc、包名）单独登记，未按 Kotlin 的 `when`/trailing lambda 直接映射。

分类实现中三个实测得出的判定约束：

- 限定表达式只有**光标落在选择器一侧**才是成员访问；落在接收者一侧是普通位置。
- 初始化槽位只看**直接父节点**：`CjNamedFunction` 本身也是 `CjDeclarationWithInitializer`，沿祖先链上溯会把整个函数体误判成初始化位置。
- 成员访问判定必须**穿过 `CjContainerNode`**：条件与分支体常挂在容器之下。

测试 `CaCompletionPositionContextTest`（3 个用例，`code-insight/completion/impl-shared/test`）覆盖成员访问两侧的区分、函数体位置与 import 位置。

```bash
./gradlew :code-insight:completion:impl-shared:test --tests '*CaCompletionPositionContextTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL（3/3）**。测试基建依赖 `:tests:test-infrastructure` 已加入 `impl-shared`。

## 8.1 / 8.2 / 8.3 / 8.6 补全模块骨架与调度（骨架已编译，行为未验证）

建立三个上游模块并接入 `settings.gradle.kts`、`docs/module-catalog.md`、`code-insight/README.md`：

- `:code-insight:completion:contracts`：`CaCangJieCompletionService`（跨模块唯一服务契约）、`CaCompletionRequest`（只保存 PSI/区间/计数，**不持有会话、符号、类型或分析序列**）、`CaCompletionSnapshot`（不可变展示数据）、`CaCompletionItemKind`、`CaCangJieCompletionServiceRegistry`（项目级注册表）。
- `:code-insight:completion:impl-shared`：`CaCompletionSection` / `CaCompletionResultSink` / `CaCompletionRunner` 与 `CaSerialCompletionRunner`；平台入口基类 `CaCangJieCompletionContributor`；平台参数翻译 `CaPlatformCompletionRequest`。
- `:code-insight:completion:impl-cfir`：`CfirCompletionSection` 基座（分析内完成语义渲染，只推快照）。
- 两种制品：`cangjie-frontend-code-insight-completion-for-ide`（fat jar，显式合并三模块）与 `-module`（源码桥接）。

设计约束在代码中被强制表达：

- 执行优先级（`executionPriority`）与展示权重分离；区段只推快照。
- 串行是唯一启用路径，并行需先过会话隔离/确定顺序/取消/等价四项准入。
- 区段**不得吞异常后返回空批次**：空结果与失败必须可区分，否则真实缺陷会伪装成「没有候选」。
- 平台入口在服务未注册时**不产出候选**，明确暴露「未接入」而不是用词补全冒充语义补全。
- 前缀由源码按光标位置自行截取，不直接采用平台 prefix：平台不同入口提供的 prefix 可能为空，直接采用会让同一位置在不同触发方式下得到不同替换区间。

```bash
./gradlew :code-insight:completion:api:compileKotlin \
          :code-insight:completion:impl-shared:compileKotlin \
          :code-insight:completion:impl-cfir:compileKotlin --offline --console=plain
```

结果：**BUILD SUCCESSFUL**。

```bash
./gradlew validateDocumentation --offline --console=plain
```

结果：**BUILD FAILED，16 处违规**。在基线工作树 `baseline-b1063824`（detached `b10638242`，无本次改动）执行同一任务得到**完全相同的 16 处违规**（`docs/module-catalog.md: missing Gradle modules :compiler:cli, :compiler:cli:cli-base` 以及工作树缺少相邻 `deveco`/`intellij-ide` 目录导致的链接校验）。新增的 completion 模块**未**出现在违规列表中，说明目录文档条目正确且未引入新违规。

**8.1 尚未完成**：模块已建且可编译，但生产 XML 接线（8.7）、实际候选生成与端到端补全均未实现，按设计「骨架不算功能完成」，因此 8.1–8.6 暂不勾选。

## 5.3 use-site 可见性适配（通过）

`CaCfirVisibilityChecker` 删除自维护的 public/internal/private/protected 分支，改为构造 `CfirAccessContext` 委托编译器唯一的 `CfirAccessibilityChecker`：

- 传入服务需要的完整 use-site 事实：使用点文件、外围声明链、接收者类型（当前 Analysis 侧未提供显式 receiver，保留为 `null` 并在 KDoc 说明）、查找来源。
- 按声明种类选择 `checkCallable` / `checkClassLike` 与 `CfirAccessKind`（class-like → TYPE，extend → EXTEND，字段 → NAMED_VALUE，其余 → CALLABLE）。
- **唯一保留在本层的规则是 `Visibilities.Local`**：它是词法可见时机问题，编译器服务按 `else -> false` 处理，不表达局部声明的时间边界。该规则连同删除的三个辅助函数都在 KDoc 中写明原因。

测试 `AnalysisApiCfirUseSiteVisibilityTest`（3 个用例 + 3 个夹具，`analysis/analysis-api-cfir/testData/components/useSiteVisibility`）：private 字段在其它类型不可见、在 owner 内部可见、internal 成员同包可见。反例夹具经官方 cjc 1.1.3 确认报 `can not access field 'secret'`，与本仓判定一致。

```bash
./gradlew :analysis:analysis-api-cfir:test --tests '*AnalysisApiCfirUseSiteVisibilityTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL**。

合并窄集：**229 tests / 0 failures**。

过程中一次合并运行出现 `CfirIdeNormalAnalysisCodeFragmentModuleTypeScopeTestGenerated > testTypeScopeQueries` 失败（`Symbol was not restored: CaCfirConstructorSymbol`）。单独重跑该套件通过，重跑合并窄集也通过，因此判定为**同 JVM 内跨套件缓存/顺序相关的偶发**，不是本次改动的确定性回归。该偶发尚未定位根因，已记为待查项。

## 5.1 / 5.2 extend 候选适用性 A4（通过）

新增公开契约 `analysis-api/.../completion/CaCompletionExtensionCandidateChecker.kt`：

- `CaExtensionApplicabilityResult` 四态：`Applicable(substitutor)`、`ReceiverMismatch`、`ConstraintUnsatisfied`、`Unknown`。
- KDoc 明确它与 `CaCompletionCandidateStatus` 三态**不是超集关系**：三态回答「名字能否直接用/是否需导入/是否不可见」，本结论回答「该 extend 在此 receiver 上是否成立」。一个候选可以完全可见但不适用。
- `Unknown` 的语义被显式规定：**不等于不适用**，消费方不得据此过滤。

实现 `CaCfirCompletionExtensionCandidateChecker`（analysis-api-cfir），并按 A9 联动 `CaSession` / `CaBaseSession` / `CaCfirSession` 完成装配。实现要点：

- 放行只用 `findExtendDeclarationSubstitution`（在 receiver 上界与直接父类型链上匹配目标模式**并**校验 extend 约束）；失败分类才用 `createExtendDeclarationSubstitutionForConstraintDerivation` 区分「目标不匹配」与「约束不成立」。绝不用宽松入口放行。
- **必须先把 extend 声明推进到 `CfirResolvePhase.TYPES`**：目标类型是 `CfirUserTypeRefImpl` 惰性引用，未推进时 `coneTypeOrNull` 为 `null`，所有匹配都会退化成 `Unknown`。这是实测定位到的关键前置条件。
- 接收者为错误类型/问号类型/存根类型时返回 `Unknown`，既不判不匹配也不判适用。
- compiler 替换结果不保证暴露类型参数映射，因此公开投影统一为 `CaCfirGenericSubstitutor`，KDoc 写明调用方不能据此读回实例化结果。

测试 `AnalysisApiCfirExtensionApplicabilityTest`（4 个用例 + 4 个夹具，`analysis/analysis-api-cfir/testData/components/extensionApplicability`）：完全实例化 extend 适用、泛型 extend 适用并携带有效替换器、目标类型不匹配给出确定的 `ReceiverMismatch`、非 extend 符号返回 `Unknown` 且不影响同点适用性。夹具中两个反例为**语义错误**（`'unwrap' is not a member of class 'Crate'`、`undeclared identifier`），已确认不是语法错误，符合「语法错误属解析测试」的约定。

```bash
./gradlew :analysis:analysis-api-cfir:test --tests '*AnalysisApiCfirExtensionApplicabilityTest*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL**。含 A4 的合并窄集：**222 tests / 0 failures**（新增 extend provider 套件回归一并通过）。

## 6.x 期望类型 A7（通过）

`CaCfirExpressionTypeProvider.expectedType` 扩充为按约束来源确定度排序的链路：调用实参/return（既有）→ 显式类型初始化器、赋值右侧、命名参数默认值 → `if`/`while` 普通条件 → 带 `else` 的 `if` 分支与 `match` 分支块 → 不完整调用的候选恢复。

关键实现约束（均经测试固定）：

- `if` 的条件、分支体等常挂在 `CjContainerNode` 之下，必须穿过容器再比对槽位，否则会漏掉确定存在的期望类型。
- 分支结果传播在遇到「约束来源」时终止：初始化器声明类型、命名参数声明类型、`return` 所属函数返回类型；块非末项、无 `else` 的 `if`、无外部约束的 `match` 分支一律 `null`。**不复制 Kotlin「无外部期望类型时取兄弟分支实际类型」的策略**，仓颉在这些位置按最小公共父类型求值。
- 未标注类型的声明属于推导方向，不能把初始化器类型当作期望类型。
- 候选恢复只使用 `CaCallInfo.calls`（成功调用或错误调用的 `candidateCalls`），按 `CaFunctionCall.valueArgumentMapping` / `CaEnumConstructorCall.payloadArgumentMapping` 取类型；任一候选缺映射、含错误类型/未定类型变量，或候选间 `semanticallyEquals` 不一致时返回 `null`。

夹具与生成：共享基类 `AbstractExpectedExpressionTypeTest` 增加 `<expr>…</expr>` 选择定位（无选择时仍用 caret，保持既有 `propertyInitializer` 的 callee 定位不变）。新增合法夹具 8 个并执行真实生成任务：

```bash
./gradlew :analysis:analysis-api-cfir:generateTestGeneratorTests --offline --console=plain   # BUILD SUCCESSFUL
./gradlew :analysis:analysis-api-cfir:test --tests '*ExpectedExpressionTypeTestGenerated*' --offline --console=plain
```

结果：**98 tests / 0 failures**（7 种模块配置 × 14 个用例）。所有新夹具均经官方 cjc 1.1.3 验证可编译；`ifBranchWithoutElse` 最初写成 `let chosen: User = if (flag) { … }`，被官方编译器拒绝（无 `else` 的 `if` 为 `Unit`），已改为语句位置形式。

错误态恢复测试 `AnalysisApiCfirExpectedTypeRecoveryTest`（4 个用例 + 4 个编辑态夹具）：唯一候选恢复 `completion/expectedtype/Alpha`；重载参数类型不一致、无保留候选、无实参表达式三种情况均返回 `null`。

窄集合并回归：**BUILD SUCCESSFUL，217 tests / 0 failures**（期望类型生成套件 + 恢复测试 + 签名 + 作用域 + 副本分析 + 70 个 dangling 生成用例 + 文件/类型作用域生成套件 + 既有候选三态执行测试）。

## 2.x 副本分析与模式控制（部分通过）

已落地 `analyzeCopy`、按文件/线程隔离且可嵌套的模式覆盖（finally 恢复）、provider 消费覆盖、`unwrapCopy` 统一副本来源，以及 `AnalysisApiDanglingFileExecutionTest`（6 个用例、6 个夹具）。

窄集（上游工作树）：

```bash
./gradlew :analysis:analysis-api-cfir:test \
  --tests '*AnalysisApiDanglingFileExecutionTest*' --tests '*ScopeContextExecutionTest*' \
  --tests '*AnalysisApiSignatureExecutionTest*' --tests '*DanglingFileCollectDiagnosticsTestGenerated*' \
  --tests '*FileScopeTestGenerated*' --tests '*TypeScopeTestGenerated*' \
  --tests '*AnalysisApiCfirComponentExecutionTest*' --tests '*ImportGroupReference*' --offline --console=plain
```

结果：**BUILD SUCCESSFUL，119 tests / 0 failures**（含 70 个 dangling 生成用例、文件/类型作用域生成套件、既有候选三态与导入计划执行测试）。

### 过程中发现并修复的回归

初次实现把 `AbstractDanglingFileCollectDiagnosticsTest` 改成「仅在 `IGNORE_SELF_MODE` 时设置 `originalFile`」（对齐 Kotlin 参考），使 3 个夹具改走 PREFER_SELF，结果 21 个生成用例失败（`getNotNullValueForNotNullContext` Failed requirement）。已核对 `b10638242` 基线全部通过，确认是本次引入。处理：回退该行为并在 KDoc 记录原因与恢复条件。

同时回退 `CaCfirSessionProvider` 中「不稳定副本绕过 Analysis 会话缓存」的改动：绕过并不能解决失效问题，还会触发同一 LL 缓存缺陷；保持模块缓存不变，并在代码注释说明修复必须落在 LL 的副本失效链路上。

### 已知缺口（2.4 / 2.5 未完成）

1. **dangling 副本 PREFER_SELF 非局部解析返回 `null`**：副本内顶层声明在 PREFER_SELF 下不可见；IGNORE_SELF 正确落到原件。这是低层 dangling PREFER_SELF 路径缺陷，不是 Analysis 投影问题。
2. **副本 PSI 改动不触发会话失效**：修改副本后 Analysis 会话与指针继续复用旧快照（指针对应到原件函数）。Kotlin 通过让不稳定副本绕过上层缓存解决，但仓颉侧绕开会撞上第 1 条的 LL 缓存缺陷。
3. 本测试环境会把文档修改同步提交到 PSI，无法构造真正的「未提交文档」状态；用例改为验证「副本文本取自当前文档」与「analyzeCopy 不回写原文件」。

## 3.x 位置与导入作用域（部分通过）

新增公开契约 `analysis-api/.../components/CaScopeContext.kt`（`CaScopeContext`/`CaScopeWithKind`/`CaScopeKind`/`CaImplicitReceiver`）、`CaScopeProvider` 上半新增 `scopeContext(position)`、`importingScopeContext`、`asCompositeScope`、`compositeScope`，以及 CFIR 侧 `CaCfirScopeContext`、`CaCfirCompositeScope` 与 provider 实现。

```bash
./gradlew :analysis:analysis-api:compileKotlin --offline --console=plain     # BUILD SUCCESSFUL
./gradlew :analysis:analysis-api-cfir:compileKotlin --offline --console=plain # BUILD SUCCESSFUL
./gradlew :analysis:analysis-api-cfir:test --tests '*ScopeContextExecutionTest*' --offline --console=plain
```

结果：**4/4 PASS**（`lexicalScopeContextAtPosition`、`overloadsKeepIdentity`、`memberScopeExposesImplicitReceiver`、`importingScopeContextIsIndependentFromFileScope`）。4 个夹具 `.cj` 全部经官方 cjc 1.1.3 编译通过。

实现要点：

- 解析塔快照只含词法层与隐式接收者，不含文件级查找层；`scopes` 因此为「解析塔层（倒序，高优先级到低优先级）+ 文件级层」。实测 kinds 为 `[LOCAL_SCOPE, LOCAL_SCOPE, FILE_SCOPE, PACKAGE_MEMBER_SCOPE]`，优先级单调。
- 无 import 声明的文件在 IMPORTS 阶段没有绑定记录，直接调用 `createFileLookupScopes` 会抛 `File import bindings are missing`。实现按 `importBindingStore.getBindings(file) != null` 分支：无绑定时只暴露文件顶层声明、包成员与内建类型，不退回 provider 现场重放导入。
- 隐式接收者剔除 `producesInapplicableCandidate()` 的不可用接收者；权限判定仍由可见性组件在使用点给出。

### 已知缺口（3.4 / 3.6 未完成）

1. **块内局部声明未进入解析塔快照**：在 `let localValue = value` 之后的位置，两层 `LOCAL_SCOPE` 名称集合分别为空与只含参数，`localValue` 不可见。测试以显式断言记录该现状，底层修复后须改为正例断言。
2. **同名重载被判为同一符号**：`CaCfirSymbolRelationProvider.isEquivalentTo` 依赖 `publicSymbolCacheKeyOrNull`，非局部 `CfirNamedFunctionSymbol` 的键只含 `callableId`（包名 + 短名），不含参数列表，两个重载因此等价。该缺陷同时影响既有三态 `checkCompletionCandidate` 的去重。本次的 `CaCfirCompositeScope` 改用声明 PSI 去重以绕开，但**符号身份模型本身需要单独修复**，不得靠调用方规避。

`3.4`（static/instance 成员查询）未实现且未加入公开接口，避免暴露无法兑现的契约。

## 第 10 组续作：Lookup / 排序 / 过滤 / 插入 / 导入（2026-10-04）

前会话在修复 `CaImportPlanApplier` 编译错时被推理网关错误中断；本次从该断点接续，完成第 10 组的实现接线与首批测试。

### 断点修复

- `CaImportPlanApplier.kt`：类体内的 `private const val NEW_LINE` 违反 Kotlin 规则（`const` 只能位于顶层/对象/伴生对象），且 `0x0A` 是 `Int` 而非 `Char`；移到文件顶层并改 `'\n'`。三模块编译（含测试源集）恢复全绿。
- 同文件：`insertImport` 在文档缺失时静默返回，但 `apply` 仍把路径记入 `added`——报告会声称「已写入」而文档未变，把基础设施错误伪装成成功。改为显式抛错（调用方必须传编辑器中的文件）。

### 接线（此前只有契约与散件，未接入调用链）

- `CaCangJieCompletionContributor.toLookupElement` 改为走 `CaLookupElementFactory.create`：图标、尾文本、插入处理器、平台优先级与快照回挂由此统一。
- `CfirCompletionService.complete` 固定「按身份去重 → 期望类型过滤 → 按分析期权重稳定排序」；过滤器注入式（默认 `CfirExpectedTypeCompletionFilter`）。去重放在过滤前（类型位置的类型建议同时来自关键字与顶层区段），排序放在过滤后（过滤会按匹配结论重写权重）。
- documentStamp 全链路：`CaCompletionRequest.documentStamp`（取**原始文件**修改戳，`caCompletionRequestFrom` 捕获；分析副本的戳与用户文档无关）→ 四个区段写入快照 → `CaInsertionValidator` 比对；`CaCompletionInsertion.documentStamp` 由 `Int` 改为 `Long`（平台修改戳是 `Long`，截断会产生比较假阳性）。`CaTestCompletionRequest` 同步补覆写。

### 过程中修掉的真实缺陷

1. **平台 `LookupElementBuilder` 在本版本不可变**（javap 实测：字段全 `final`，`with*` 走 `copyPresentation()` + `cloneWithUserData(...)` 返回新实例）。工厂把 `withTailText`/`withTypeText` 当语句调用、丢掉了返回值——**尾文本与类型文本在生产代码里静默丢失**，弹窗里同名重载长得一模一样。已改为接住返回值；`CaLookupElementFactoryTest.mapsSnapshotFields` 锁定。
2. **导入插入点语义**：原实现插在行终止换行之后，import 区后有空行时新 import 会被推过空行、贴到后续声明上。改为插在换行**之前**（配合写入文本自带的开头换行），新 import 与上一条相邻成行。`addsMissingImportAfterLastDirective` 锁定。
3. 插入校验与过滤器在接线前的行为契约由测试固定：关键字免版本戳、过期/越界拒绝写入；BASIC 不查期望类型、SMART 未知时与 BASIC 同集合、TYPE 位置只留类型身份、触发次数不参与过滤。

### 新增测试与执行结果（一次一个测试类，逐轮读 XML）

| 测试类 | 模块 | XML 结果 |
|---|---|---|
| CaLookupElementFactoryTest | impl-shared | tests=9 failures=0 errors=0 |
| CaInsertionValidatorTest | impl-shared | tests=5 failures=0 errors=0 |
| CaPsiImportPlanApplierTest | impl-shared | tests=6 failures=0 errors=0 |
| CaCompletionPositionContextTest（回归） | impl-shared | tests=3 failures=0 errors=0 |
| CfirExpectedTypeCompletionFilterTest | impl-cfir | tests=4 failures=0 errors=0 |
| CfirKeywordCompletionSectionTest（回归） | impl-cfir | tests=4 failures=0 errors=0 |
| CfirLocalScopeCompletionSectionTest（回归） | impl-cfir | tests=4 failures=0 errors=0 |
| CfirMemberCompletionSectionTest（回归） | impl-cfir | tests=4 failures=0 errors=0 |
| CfirTopLevelCompletionSectionTest（回归） | impl-cfir | tests=4 failures=0 errors=0 |

命令形如（逐类执行，`--rerun` 保证真实执行）：

```bash
./gradlew :code-insight:completion:impl-shared:test --tests '*CaLookupElementFactoryTest*' --rerun --offline --console=plain
```

每轮从 `build/test-results/test/TEST-*.xml` 核对 `tests=` / `failures=` / `errors=`（结果目录逐轮覆盖，必须跑完一类读一次）。平台环境注意：轻量夹具不注册 `EditorFactory`；PSI 文档写入要求 EDT + 命令上下文（测试用 `EventQueue.invokeAndWait` + `WriteCommandAction`）。

### 新增夹具与 cjc 取证

`impl-cfir/testData/filter/` 四个夹具（basicPassesThrough / smartKeepsBasicSetWhenExpectedTypeUnknown / smartFiltersAtTypePosition / smartExpressionMatchPromotesWeight）经官方 cjc 1.1.3 逐文件验证（`--output-type=staticlib`，TMP/TEMP 指向 D 盘）：均只报 `undeclared identifier 'completionTarget'`（语义错误），无语法错误，符合编辑态夹具约定。

### 勾选回写

- 勾选：**10.1**（lookup 工厂 + 接线 + 9/9 测试）、**10.4**（SMART 过滤 + 未知降级 + 四条契约 4/4 测试）。
- 未勾选（部分完成，缺口明确）：
  - **10.3**：权重折算（来源 > 导入成本 > 类型匹配 > 声明次序）与稳定排序已测；设计 B6 列出的**类别/弃用/参数匹配维度未进权重公式**。
  - **10.5 / 10.7**：三种导入策略、组织名/alias、冲突退回限定名、幂等均已测（6/6）；**插入路径接线待候选生产者**（当前无区段产出 AddImport 计划），单次撤销仅结构性成立。
  - **10.6**：插入处理器已实现（括号/已填实参/命名冒号/caret）；**端到端触发与插入未验证**（待 14.2 host fixture）。
  - **10.2**：版本/区间校验已实现并测试；**`CaCompletionRecoveryService` 的 pointer 恢复实现仍缺**（接口已就位，无实现）。
  - **10.8**：工厂/校验/应用器/过滤器单元级测试已补；**「真实触发并选择」的端到端测试仍缺**。

## 尚未执行的准入

- G1/G2 Analysis 语义、G3 原生闭环、G4 高级场景、G5 制品/平台矩阵均未通过。
- 没有启动 IDE 沙箱、没有启用 LSP/native/SMART/并行开关。

## G0 前置与 D-5 上游侧修复（2026-10-04）

本节只覆盖**上游工作树**（`claude/completion-k2-musing-bun`）。`intellij-ide` 与 `deveco` 两个出货宿主按分工由别的会话处理，本工作区不写入。

### 任务 1.6 —— `Ca` 前缀下线的 27 个重命名已提交

- 提交 `133851357 refactor(completion): code-insight 层下线 Ca 前缀`。
- `git log --diff-filter=R --oneline -1 133851357` 可见重命名记录，G0 前置解除；`design.md` §4.4 的文件清单不再随工作树重建漂移。
- 随该提交一并落定的显式决策（任务 8.9 的命名面）：包名统一为 `org.cangnova.cangjie.ide.completion.*`；**code-insight 层不用 `Ca` 前缀**；Analysis API 的 `CaSession` / `CaCallableSymbol` / `CaType` / `CaValueParameterSymbol` 等跨层类型保留前缀不变——它们不属于本层命名。

### 任务 8.8 上游侧 —— 机械重命名两处漏改

1. `impl-cfir/.../ngJieCfirCompletionContributor.kt`：批量去前缀脚本把本就叫 `CangJieCfirCompletionContributor` 的文件当成 `Ca` + `ngJie...` 处理。已 `git mv` 回 `CangJieCfirCompletionContributor.kt`，与类名及生产描述符 `code-insight/completion/impl-cfir/resources/META-INF/code-insight/cangjie-code-insight-completion.xml` 的 `implementationClass` 恢复一致。**文件名此前与描述符不一致，属于静默缺陷**：描述符按 FQN 解析所以仍能加载，但按文件名检索的实现会漏掉它。
2. `impl-shared/test` 5 个测试类名去前缀（文件名此前已改、类名漏改）：`CaCompletionPositionContextTest`→`CompletionPositionContextTest`、`CaPsiImportPlanApplierTest`→`PsiImportPlanApplierTest`、`CaCangjieInsertionTest`→`CangjieInsertionTest`、`CaInsertionValidatorTest`→`InsertionValidatorTest`、`CaLookupElementFactoryTest`→`LookupElementFactoryTest`。逐个确认无交叉引用后原地替换。

### 执行记录

```bash
./gradlew :code-insight:completion:impl-shared:compileTestKotlin :code-insight:completion:impl-cfir:compileTestKotlin
# BUILD SUCCESSFUL in 1m 30s（137 actionable tasks: 7 executed, 130 up-to-date）
```

```bash
./gradlew :code-insight:completion:impl-shared:test
./gradlew :code-insight:completion:impl-cfir:test
```

| 测试类 | 模块 | XML 结果 |
|---|---|---|
| CompletionPositionContextTest | impl-shared | tests=3 failures=0 errors=0 |
| PsiImportPlanApplierTest | impl-shared | tests=9 failures=0 errors=0 |
| CangjieInsertionTest | impl-shared | tests=8 failures=0 errors=0 |
| InsertionValidatorTest | impl-shared | tests=5 failures=0 errors=0 |
| LookupElementFactoryTest | impl-shared | tests=9 failures=0 errors=0 |
| CfirExpectedTypeCompletionFilterTest | impl-cfir | tests=4 failures=0 errors=0 |
| CfirCompletionRecoveryServiceTest | impl-cfir | tests=5 failures=0 errors=0 |
| CfirKeywordCompletionSectionTest | impl-cfir | tests=4 failures=0 errors=0 |
| CfirLocalScopeCompletionSectionTest | impl-cfir | tests=4 failures=0 errors=0 |
| CfirMemberCompletionSectionTest | impl-cfir | tests=4 failures=0 errors=0 |
| CfirTopLevelCompletionSectionTest | impl-cfir | tests=4 failures=0 errors=0 |

合计 11 类 59 例全绿。较上一节记录的 9 类 43 例增加 16 例，来自第 10 组续作之后的接线：`CangjieInsertionTest`(8) 与 `CfirCompletionRecoveryServiceTest`(5) 为新类，`PsiImportPlanApplierTest` 6→9、`LookupElementFactoryTest` 7→9。

### 静态核对

- `code-insight` 下 `class|interface|object Ca[A-Z]` 声明清零。
- 21 个旧名（`CaLookupElementFactory`、`CaImportPlanApplier`、`caCompletionSnapshot` 等）在 `code-insight`/`analysis`/`lsp`/`compiler`/`common`/`psi` 中已无残留引用。
- 未暂存改动复核为纯机械重命名：抽查 `CfirSnapshotFactory.kt` 的 diff，去掉 `Ca` 的只发生在 `org.cangnova.cangjie.ide.completion.*` 下的导入与声明，Analysis API 类型的导入行原样保留。

### 未覆盖边界

- 8.8 的验收项「`intellij-ide` 与 `deveco` 两个生产描述符中 contributor 均唯一且可解析」属宿主侧，按分工不在本工作区范围，故 8.8 保持未勾选。
- 本节只证明**重命名不改变行为**（编译 + 既有测试全绿），不构成对第 2–15 组任何任务的验收。

## 缺陷 D-2：同名重载被判为同一符号（2026-10-04）

提交 `f6b306951 fix(analysis+completion): 重载签名进入 public symbol 缓存键`。本节只覆盖上游工作树。

### 根因

`CaCfirCallableSymbolCacheKey(callableId, kind)` 与 `CaCfirExtendMemberCallableSymbolCacheKey(extendIdentity, callableName, kind)` 都不含参数列表，而 `CallableId` 只由包名与短名构成。任意两个同名重载因此得到**完全相等**的键。

后果有两处，都不是显示层问题：

1. `CaCfirSymbolRelationProvider.isEquivalentTo` 直接比较缓存键（`CaCfirSymbolRelationProvider.kt:297-301`），对两个不同声明返回 `true`；
2. 恢复端 `matchesStableCallable` 只比 `callableId` 与 kind（`CaCfirPublicSymbolRestore.kt:179`），两个重载都命中，`singleOrNull` 因「命中多个」返回 `null`——不是选错，是**两个符号一起丢失**。

`CaCfirTopLevelFunctionSymbolPointer` 的文档注释本来就写着「与 Kotlin FIR 一样，真正区分重载的是 callable signature，不是 `CallableId` 本身」，但它走的是另一条 `restoreTopLevelFunctionPublicSymbol` 通道；通用通道一直没跟上。

### 修法

- 两个键各增一个 `CfirCallableSignature?` 分量（参数类型、类型参数数量、返回类型）；
- `publicSymbolCacheKeyOrNull` 统一经 `stableCallableSignatureOrNull()` 取值，非 `CfirCallableDeclaration` 形态返回 `null`（那种情况不存在重载）；
- `restoreCallablePublicSymbol` 与 `matchesStableCallable` 按签名筛选；`restoreExtendMemberCallablePublicSymbol` 的 `expectedKey` 带上签名；
- 两个 pointer 把键里的签名透传给恢复端。

形状对齐 Kotlin FIR 的 `KaFirCallableIdPlusSignature`。已知代价：取签名会 `lazyResolveToPhase(CfirResolvePhase.TYPES)`，这是取值本身必需的，Kotlin FIR 同理。

### 插件侧同源

- `callableSnapshot` 的 `deduplicationKey` 由另写一份公式改为直接调用 `callableIdentityKey`——原代码两处各自算 `if (tailText.isEmpty()) name else "$name$tailText"`，正是该文件注释警告的「两处算法不同会把同一成员按 MEMBER 和 EXTEND_MEMBER 各补一次」的隐患。
- `callableIdentityKey` 改由「展示名 + 参数类型 + vararg 形态 + 返回类型」构成，不再复用带 `!` / `vararg` / `= ...` 的展示尾文本：那三个是**调用形态标记**（命名实参、默认值），属展示约定，拿它当身份会让同一声明因展示格式变化被当成两个候选。

**未做**：「按声明 PSI 去重」在本仓找不到对应实现。`completionDecisionKey()` 定义在 `CaCfirPublicSymbolKeyMapping.kt:175` 但**没有任何消费方**，插件侧也未见按 PSI 去重的代码。若该绕行指别处，需指明后补。

### 取证

夹具 `analysis/analysis-api-cfir/testData/equivalence/overloadIdentity.cj`（顶层 / 类成员 / extend 成员各一组重载）经官方 cjc 1.1.3 验证：`--output-type=staticlib` 下**零错误**，只有 unused variable / unused function 两条警告；末尾 `LLVM ERROR: Broken global value found` 是 cjc 后端对同文件 `extend` 本地类的崩溃（去掉 `main` 仍复现），与前端无关。反倒是既有夹具 `symbolEquivalence.cj` 在同一命令下报 3 个语义错误。

夹具 `code-insight/completion/impl-cfir/testData/sections/topLevelOverloadsCarryDistinctSignatures.cj` 经 cjc 1.1.3 验证：唯一错误是 `undeclared identifier 'completionTarget'`，即补全占位符的预期形态。

### 对照实验（证明新用例真能抓住缺陷）

把两个键的 `signature` 临时置为 `null`（等价于修复前行为）后重跑：

```
AnalysisApiSymbolEquivalenceTest: tests=2 failures=1
overloadIdentity: AssertionFailedError: 顶层函数 的两个重载被判定为同一符号：format
                   ==> expected: <false> but was: <true>
symbolEquivalence: 通过
```

既有的 `symbolEquivalence` 在缺陷存在时**照样通过**——这解释了为什么该缺陷此前没被现有套件拦住。随后按备份原样恢复。

### 执行记录

```bash
./gradlew :analysis:analysis-api-cfir:test --rerun
```

472 个测试类、**1616 用例、failures=0、errors=0**（按 `build/test-results/test/TEST-*.xml` 聚合核对，不以 `BUILD SUCCESSFUL` 为据）。

```bash
./gradlew :analysis:low-level-api-cfir:test --rerun
```

57 个测试类、136 用例，**4 个失败**：`CfirCjmpCallerFirstDiagnosticsTest` 的
`defaultArgumentInferredCallerFirst` / `shadowedCommonCallerFirst` / `defaultArgumentCallerFirst` / `shadowedCommonInferredCallerFirst`，
报 `CFIR_NOT_MATCHED`（两个带 `CFIR_AMBIGUOUS_FUNCTION_CALL`）。

**这 4 个是本工作树的既有失败，与本次改动无关**：在签名置空（修复前行为）下重跑同一测试类，结果同样是 `tests=4 failures=4`，失败用例名与消息逐条相同。该基线此前没有记录，本节补上。

```bash
./gradlew :code-insight:completion:impl-cfir:test --rerun
```

6 个测试类、26 用例、failures=0（含新增 `topLevelOverloadsCarryDistinctSignatures`）。

### 平台事实

- `analysis/low-level-api-cfir/tests-gen/**` 下 26 个生成文件在跑测试后被改写，但 `git diff --ignore-cr-at-eol` 为空——**只有 Windows 行尾差异，零内容改动**，不纳入提交。判断生成产物是否真有变化要用 `--ignore-cr-at-eol`，否则会被行尾噪声淹没。
- 测试框架要求**方法名 == 夹具文件名**，新增测试必须同时新增 `testData` 文件，否则报 `Cannot find test file`。

## 缺陷 D-1：块内 `let` 局部声明不可见（2026-10-05）

提交 `3b1f2fdcf fix(analysis): 块内 let 绑定的入作用域不再被 tower 清理回滚`。本节只覆盖上游工作树。

### 根因与任务描述的偏差（重要）

任务 3.10 把根因写成「`CfirTowerDataContext.addLocalVariable` 在无活跃局部 scope 时静默丢弃」。**实测不是这一处**：

- 该早退**确实会发生**（诊断打印 `addLocalVariable DROPPED CfirValueParameterImpl`），但丢的是**函数参数**，随后由 `withFunctionLocalScope` 的 `preloadValueParameters` 补回，因此不是本症状的成因；
- 真正的原因是 `ContextCollector` 把绑定变量的入作用域放在了 `visitPatternVariable` 的 `withLocalVariableBodyCompat` **内部**，而该 compat 就是 `withTowerDataCleanup`，退出时把 tower 恢复成进入前的状态，**这次存储被一并回滚**。

诊断打印的时序是决定性证据：

```
STORE CfirPatternBindingVariableImpl locals=2       ← 确实存进去了
isActive=false at kind=BODY psi=completionTarget
dump block BODY locals=[[param], []]               ← 但块快照里没有它
```

表现：解析与补全都看不见块内 `let` 局部变量，**且不报任何诊断**——与「作用域为空」的静默失败一致。

### 修法

存储移到各自的清理之外：

| 位置 | 改法 |
|---|---|
| `visitPatternVariable` | `onActive{}` 之后按 `pattern.bindingVariables()` 逐个 `storeVariable` |
| `visitPatternBindingVariable` | `visitVariableLike` 之后再 `storeVariable` 一次 |
| `visitFieldVariable` | 同上 |
| `visitVariableLike` | 不再负责存储，KDoc 写明由调用方在清理之外做 |

### 第一版改法引入的回归（记录下来，避免重犯）

第一版只保留 `visitPatternVariable` 的统一存储，**删掉了 `visitVariableLike` 里的存储**，结果 `match` 分支的绑定消失了——`case Year(y) => y` 的 `y` 可以**不经 `CfirPatternVariable` 直接走到 `visitPatternBindingVariable`**。

被 `ContextCollectorTest.matchPatternBindingBranchScopes` 与 `...InCopiedFile` 两例当场抓住（`expected: <1> but was: <0>`）。补回 `visitPatternBindingVariable` 的存储后恢复。**教训**：把存储从「叶子访问器」上移到「容器访问器」时，必须先确认叶子节点没有绕过容器的独立入口。

### 两处负例断言翻转为正例

两处的注释原本都写着「底层修复后此处应断言包含 …」，本轮一并翻转：

- `ScopeContextExecutionTest.lexicalScopeContextAtPosition`：由 `assertTrue(localsNow.isEmpty())` 改为 `assertTrue(locals.contains("localValue"))`；
- `CfirLocalScopeCompletionSectionTest.localScopeCandidates`：由 `assertFalse(names.contains("local"))` 改为 `assertTrue(...)`。

**补全侧夹具必须同时改**：原夹具的补全点在 `let local = param` 的**初始化表达式**里，那一刻 `local` 尚未声明，负例断言在该位置本来是对的。已把 4 个夹具的补全点移到声明之后（`let local = param` → `completionTarget`）。另注意该套件**每个测试方法各有自己的夹具文件**（方法名 == 文件名），改一个不够，四个都要改——漏改的表现是 `NoSuchElementException`，不是断言失败。

### golden

`testData/contextCollector/simple*.txt` 四个文件重生成，Element 1 新增：

```
+            Variables:
+                CfirPatternBindingSymbol <element: CfirPatternBindingVariable>
```

**副作用**：`-Dupdate.test.data=true` 重写了 `contextCollector/` 下全部 golden，但 `git diff --ignore-cr-at-eol` 显示只有 `simple*.txt` 有真实内容变化，其余 16 个文件**仅 Windows 行尾差异**。判断 golden 是否真有变化必须加 `--ignore-cr-at-eol`，否则会把行尾噪声一起提交。

### 执行记录（按 XML 聚合核对）

| 套件 | 用例 | 失败 |
|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1616 | **0** |
| `:code-insight:completion:impl-shared:test` | 34 | **0** |
| `:code-insight:completion:impl-cfir:test` | 26 | **0** |
| `:analysis:low-level-api-cfir:test` | 136 | 4（见下） |

4 个失败全部是 `CfirCjmpCallerFirstDiagnosticsTest`，即「缺陷 D-2」节记录的既有基线，与本次改动无关。

### 排查方法教训

1. **诊断代码自己编译不过时，后续读到的测试 XML 是上一轮的陈旧产物。** 本轮曾两次把 `BUILD FAILED` 的运行当成有效结果读 XML，据此得出过错误结论（一度以为 `addLocalVariable` 从未被调用）。**每次读 XML 前必须先确认该次构建成功**；`grep -E 'BUILD'` 应当成为读 XML 的前置动作。
2. 诊断 `println` 要**一次性全部加完再跑**，分批加会得到互相矛盾的证据链（第一批的模块没重编，输出为空，第二批才生效）。
3. 用正则批量删除诊断行时，多行 `println(...)` 会留下**续行**导致语法错误；删完必须单独跑一次 `compileKotlin` 确认。

## 任务 3.6：作用域公开层回归 + match 分支收集侧隔离（2026-10-05）

提交 `0bd8901fe fix(analysis): match 分支在收集侧逐个隔离作用域，补齐作用域公开层回归`。本节只覆盖上游工作树。

### 新发现的缺陷：收集侧缺 match 分支作用域隔离

写 3.6 的公开层用例时撞出来的，此前无人覆盖。

编译器侧 `CfirExpressionsResolveTransformer.resolveBranch` 用 `withNewLocalScope`（即 `context.forBlock`）**逐 case 隔离**；收集侧 `ContextCollector` **没有任何 match 覆写**，走 `visitElement` 的通用遍历，所有 case 共用同一个收集作用域。

诊断取证（`dumpContext` 时刻的 `localScopes`）：

```
dump kind=SELF psi=branchTarget locals=[[shape], [radius, side]]
```

`case Circle(radius)` 的 `radius` 与 `case Square(side)` 的 `side` 挤在**同一个** `CfirLocalScope` 里。

**后果**：在 `Square` 分支内也能看到 `radius`。补全会把兄弟分支的绑定当成本分支候选，用户选中后无法通过编译。

**为什么既有测试没抓到**：`ContextCollectorTest.matchPatternBindingBranchScopes` 断言的是「两个分支里**同名**的 `y` 是不同符号」——验证的是**区分**，不是**隔离**。两个不同名的绑定漏进同一作用域，这个断言形式天然测不到。

修法：`ContextCollector` 增 `visitMatchExpression` / `visitMatchBranch`，每个 case 分支在 `withBlockScopeCompat` 里访问，分支的模式与体共享该作用域（与编译器侧同形）；分支体本身是 block 时由 `visitBlock` 再开一层。

### 新增公开层用例（每个用例各有自己的夹具，方法名 == 文件名）

| 用例 | 覆盖 | 关键断言 |
|---|---|---|
| `lexicalScopesExposeParametersLocalsAndTypeParameters` | 局部 / 参数 / 类型参数 | `T`、`value`、`second`、`local` 全部可见；函数自身不出现在其体内作用域 |
| `nestedBlocksKeepInnermostShadowing` | 嵌套块 + 同名遮蔽 | 断言**次序**：≥2 层 LOCAL_SCOPE，且最先命中的 `shadowed` 必须来自最内层 |
| `matchBindingStaysInsideItsBranch` | match 绑定 | 正例 `side` 可见；反例 `radius` 不得泄漏 |
| `scopeContextInCopiedFileIsEquivalent` | 原件 / 分析副本 | 两边 LOCAL_SCOPE 名字集合相等，且都含 `localValue`、`shadowed` |

两个断言写法上的要点：

- **遮蔽要断言次序而不是集合**。`{shadowed}` 两个都在并不算错；只有「最先命中的那个来自最内层」才代表遮蔽正确。
- **副本测试只比词法层**。本工作树的 dangling 副本在文件级查找层仍有缺陷 D-3 的缺口，早先按「全名集合相等」写断言会直接失败（原件有 `main`、副本没有），把两个独立问题绑死在一个断言里。断言只取 `LOCAL_SCOPE` 的名字。

receiver 一项由既有 `memberScopeExposesImplicitReceiver` 覆盖，未新增。

### 夹具取证

四个夹具均经官方 cjc 1.1.3 验证（`--output-type=staticlib`），只报占位符未声明：

```
undeclared identifier 'scopeTarget' / 'innerTarget' / 'outerTarget' / 'branchTarget'
```

**仓颉泛型函数语法是 `func name<T>(...)`，不是 `func <T> name(...)`**——第一次写错，cjc 报 `expected a func name after keyword 'func', found '<'`。本仓既有写法见 `analysis-api-cfir/testData/signatures/chainedFunctionAndVariableSignatures.cj`。

### 执行记录（按 XML 聚合）

| 套件 | 用例 | 失败 |
|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1620（较上轮 +4） | **0** |
| `:code-insight:completion:impl-cfir:test` | 26 | **0** |
| `:analysis:low-level-api-cfir:test` | 136 | 4（`CfirCjmpCallerFirstDiagnosticsTest` 既有基线） |

### 仍未做

缺陷 D-3（dangling 副本 PREFER_SELF 非局部解析返回 `null`）未修，仍卡着 15.1 分支 B。

## 代码片段作用域与位置分类（2026-10-05）

提交 `1cf1ca2d9 feat(analysis+completion): 代码片段作用域与位置分类（任务 3.7 / 3.8 / 3.9）`。本节只覆盖上游工作树。

### 3.9 位置分类

`CompletionPositionContexts` 增 `CODE_FRAGMENT_EXPRESSION` / `CODE_FRAGMENT_BLOCK`，判定**置于块/函数体之前**：片段内部也有 `CjBlockExpression`，按块位置处理会让片段退化成普通函数体，丢掉「无所属声明、作用域来自片段上下文」这一事实。3 例正反用例（表达式片段、块片段、反例：普通文件不得误判）。

**枚举项不是免费的**：加两个值让 `CfirKeywordCompletionSection` 的穷尽 `when` 编译失败（`'when' expression must be exhaustive`）。补两条分支后恢复——块片段同 `FUNCTION_BODY`、表达式片段同 `EXPRESSION`。`CfirExpectedTypeCompletionFilter` 的同类 `when` 本来就有 `else`，不受影响。全量回归正是靠这条发现的。

### 3.7 实现位置与设计预想不同（重要）

设计 §6.2 预设「实现 `getExtraScopes` 的桩」即可让片段自身声明可见。**实测不成立**：

`LLCfirBodyLazyResolver.resolveCodeFragmentContext` 是**先**调 `withExtraScopes()` 建上下文、**再**解析片段体。此刻 `cfirCodeFragment.block` 还是 `CfirLazyBlock` 桩，直接读 `statements` 触发

```
Caused by: java.lang.IllegalStateException: CfirLazyBlock should be resolved before accessing
    at CfirLazyBlockImpl.getStatements
    at CodeFragmentScopeProvider.getExtraScopes(CodeFragmentScopeProvider.kt:58)
```

所以片段自身声明真正发布的位置是 `ContextCollector.visitCodeFragment`。`getExtraScopes` 对未解析片段返回空列表（是**正确结论**，不是缺失），已解析片段仍按声明种类汇成成员作用域。

`CfirPatternVariable` 必须显式跳过：`CfirLocalScope.storeVariable` 对它 `error(...)`，真正入作用域的是 pattern 内部的 binding variable。

### 3.8 作用域种类

- `CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE`，对位 Kotlin `KaScopeKind.ScriptMemberScope` 但语义不同（Kotlin 侧只服务 `.kts` 脚本，仓颉没有脚本文件）。
- **`indexInTower` 取 4，不取 3**：3 已被 `STATIC_MEMBER_SCOPE` 占用。撞值不是「重复」而是**排序等价**，会让遮蔽次序的真实差异在断言里消失。
- `CfirLocalScope` 增 `isCodeFragmentMemberScope` 标记，四个 `storeXxx` 方法**全部**要透传——只改构造器的话第一次 store 就把标记丢掉。
- `toScopeKind` 在 `isLocal` 之前先判该标记：片段作用域在查找行为上是局部作用域，但对上层必须单独成类。
- **只有块片段才标记**。表达式/类型片段没有自身顶层声明，给它们挂一层空的成员作用域，会与「不泄漏宿主专有符号」的验收直接冲突（第一版没区分，两个用例都红）。

### 已知缺口（据实记录，未写成通过的断言）

1. **宿主文件顶层声明在片段内不可见**。设计 §6.2 要求「片段内可见片段自身顶层声明 + 目标文件可见符号」。实测片段上下文里只有合成片段文件自己的 `FILE_SCOPE`（无顶层声明），宿主文件的 `hostTarget` 不可见。测试注释里写明「未覆盖」并指向 `tasks.md` 3.7 遗留项。
2. **`CjCodeFragment.fragmentMemberScope` 公开入口未加**：当前经 `scopeContext` 投影得到，未另开入口。
3. **`context(session)` 桥接生成器未跑**（任务 7.5）。`CaScopeProvider` 底部有「自动生成，请勿手工修改」标记，本轮未触碰该文件的手改桥接。
4. **类型片段用例未加**：`CjTypeCodeFragment` 在本仓 PSI 侧没有对应构造入口，加之前需先确认可用性。

### 执行记录（按 XML 聚合，逐个核对 mtime 新鲜度）

| 套件 | 用例 | 失败 |
|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1622（473 类） | **0** |
| `:code-insight:completion:impl-shared:test` | 37 | **0** |
| `:code-insight:completion:impl-cfir:test` | 26 | **0** |
| `:analysis:low-level-api-cfir:test` | 136 | 4（`CfirCjmpCallerFirstDiagnosticsTest` 既有基线） |

`contextCollector` 的 golden 本轮**无真实内容变化**（`git diff --ignore-cr-at-eol` 为空），未纳入提交。

### 本轮再次踩到「陈旧 XML」陷阱

诊断过程中诊断代码自身编译不过（`CjPsiSourceElement` 的包是 `org.cangnova.cangjie.source`
而非 `.psi`；批量改 import 又留下一行错 import），我据此读了**两轮陈旧 XML**得出错误结论，
直到显式核对 XML mtime 才发现。已把「读 XML 前先 `grep BUILD` + 核对 mtime」写进记忆
`cangjie-test-run-must-verify-build`。

## 闭合 3.7 的宿主文件可见性缺口（2026-10-05）

提交 `e2d6929cb fix(analysis): 代码片段内可见宿主文件的顶层声明`。

### 缺口

上一提交（`1cf1ca2d9`）落地了片段自身顶层声明的可见性，但设计 §6.2 的验收是**两条**：
「片段内可见片段自身顶层声明 + 目标文件可见符号」。实测后一条不成立：片段上下文里
只有合成片段文件自己的 `FILE_SCOPE`，而该文件没有顶层声明，宿主文件的 `hostTarget`
在片段内不可见。

### 修法

`CaCfirScopeContext.scopes` 增 `hostFileLevelEntries()`：片段位置额外取
`CjCodeFragment.context?.containingFile` 的 CFIR，按 `CfirFileDeclaredTopLevelScope`
投影成 `FILE_SCOPE` 条目，排在片段自身文件级之前。片段成员作用域因此是**叠加**在
宿主上下文之上，而不是替换它。

非片段位置、片段锚定在自身文件内、或宿主 CFIR 构建失败时一律返回空列表——
宿主层是**补足**，拿不到时不该改变既有结果。

### 一处 API 细节

`LLResolutionFacade.getOrBuildCfirFile(CjFile)` 是 **internal**，analysis-api-cfir 取不到；
可用的是同文件里的扩展 `CjFile.getOrBuildCfirFile(resolutionFacade)`。写成成员调用形式
会编译失败。

### 执行记录（按 XML 聚合，逐套核对 mtime）

| 套件 | 用例 | 失败 | XML 新鲜度 |
|---|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1622（473 类） | **0** | 09:30:42 |
| `:code-insight:completion:impl-shared:test` | 37 | **0** | 09:31:02 |
| `:code-insight:completion:impl-cfir:test` | 26 | **0** | 09:30:58 |
| `:analysis:low-level-api-cfir:test` | 136 | 4（既有 CJMP 基线） | 09:31:45 |

本轮起每套都显式打印 newest XML 时间戳与当前时间对照，不再靠「BUILD 成功」推断结果新鲜。

## 3.8 公开入口 `CjCodeFragment.fragmentMemberScope`（2026-10-05）

提交 `5184aacb0 feat(analysis): 增 CjCodeFragment.fragmentMemberScope 公开入口`。

### 缺口与修法

此前片段成员作用域只能经 `scopeContext` 的投影拿到，没有独立入口，调用方无法直接问
「这个片段自己声明了什么」。

`CaScopeProvider` 增 `val CjCodeFragment.fragmentMemberScope: CaScope?`，CFIR 实现
**复用 `CaCfirScopeContext` 的投影**而非另写一份：取片段内最后一个引用作为查询位置
（它必然落在片段自身作用域内，调用方不必再指定位置），再从 `scopes` 里挑出
`CODE_FRAGMENT_MEMBER_SCOPE`。

新增用例 `fragmentMemberScopeMatchesScopeContextProjection` 断言两个入口给出**同一组
声明**——两处各写一套算法时，该作用域迟早只在其中一条路径上出现，这条用例就是为此存在。

非块片段返回 `null`：那是「没有这类声明」而不是「找不到」。宿主文件的顶层声明不在本
作用域内，仍需经 `scopeContext` 取。

### 三处 API 细节（都会让人卡住的）

1. `CjCodeFragment` **没有** `contentElement`——那是 `CjExpressionCodeFragment` 上的。
   直接在片段自身上找子元素即可。
2. 在 `override val CjCodeFragment.fragmentMemberScope` 里写 `this@importingScopeContext`
   会 `Unresolved label`：那是**另一个扩展**的标签，必须先把接收者收进局部变量。
3. 测试里用 `getAllPossibleNames()` 而不是 `callables { }.map { it.displayName() }`：
   `displayName` 是 impl-cfir 的扩展，analysis 层取不到；且 `callables` 返回惰性
   `Sequence`，lambda 参数类型推不出来（`Cannot infer type for this parameter`）。

### 执行记录（按 XML 聚合，逐套核对 mtime）

| 套件 | 用例 | 失败 | XML 新鲜度 |
|---|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1623（473 类，+1） | **0** | 09:53:25 |
| `:code-insight:completion:impl-shared:test` | 37 | **0** | 09:54:03 |
| `:code-insight:completion:impl-cfir:test` | 26 | **0** | 09:53:52 |
| `:analysis:low-level-api-cfir:test` | 136 | 4（既有 CJMP 基线） | 09:54:48 |

### 3.8 剩余

- `context(session)` 桥接生成器未跑（任务 7.5）。`CaScopeProvider` 底部有
  「自动生成的 context 桥接，请勿手工修改」标记，本轮未触碰任何手改桥接。
- 次序断言只覆盖作用域**种类**，未覆盖 `indexInTower` 的相对次序。

## 任务 9.1：类型参数候选（2026-10-05）

提交 `f96fe4a41 feat(analysis+completion): 类型参数走独立作用域通道，补全可给出 T`。
本节接续上一条记录（`77562d2c1`）里记下的阻塞点——那一轮只留了说明，本轮把通道做完。

### 根因

`CfirTypeParameterScopeImpl` 的两条公开通道互相矛盾：

| 成员 | 位置 | 行为 |
|---|---|---|
| `getClassifierNames()` | `:90` | **报告了**类型参数名 |
| `processClassifiersByName` | `:101` | **空实现** |
| `getCallableNames()` | `:85` | 空集 |

于是 `CaScope.classifiers {}` 与 `callables {}` 都枚举不到，而 `CaScope` 上没有类型
参数专用入口。关键在于：**解析塔里这个作用域是存在的**（`withTypeParametersOf` →
`addNonLocalScope`），`toScopeKind` 也映射到了 `TYPE_PARAMETER_SCOPE`——
**塔里有，公开层取不出**。

### 修法

- `CaScope` 增 `typeParameters` / `typeParameters(nameFilter)`；
- `cfirScopeUtils` 增 `getTypeParameterSymbols`，走 `CfirTypeParameterScope.processTypeParametersByName`；
- `CaCfirBasedScope` 的名字来源用**已有的** `getPossibleClassifierNames()`；
- `CaCfirCompositeScope` 按首次出现去重合并；`CaCfirFileScope` / `CaCfirPackageScope`
  返回空序列（类型参数只存在于泛型声明的声明体内）。

### 三个 API 形状（上一轮正是卡在这里）

1. `processTypeParametersByName` 在 **`CfirTypeParameterScope` 子类**上，不在
   `CfirScope`——通用辅助函数里必须向下转型，否则 `Unresolved reference`。
2. `buildTypeParameterSymbol` 在 **`builder.classifierBuilder`** 上，不是
   `builder` 的直接成员。
3. 类型参数名要从 `getPossibleClassifierNames()` 取；`cfirScope.getClassifierNames()`
   在 `CaCfirBasedScope` 的接收者类型（`S : CfirScope`）上不可见——上一轮写这个
   正是这个原因编译不过。

### 补全侧

`CfirLocalScopeCompletionSection` 词法层加入 `TYPE_PARAMETER_SCOPE`，经
`typeParameters` 通道枚举；身份复用 `classifierSnapshot`（`CaTypeParameterSymbol`
是 `CaClassifierSymbol`），类别记 `CompletionItemKind.TYPE_PARAMETER`——
归入 `LOCAL` 会被渲染成带括号的调用形态。`LookupIcons` 同步补图标分支：
**加枚举值必须同步所有穷尽 `when`**，本变更已因此触发过两次编译失败
（`CfirKeywordCompletionSection`、`LookupIcons`）。

### 执行记录（按 XML 聚合，逐套核对 mtime）

| 套件 | 用例 | 失败 | XML 新鲜度 |
|---|---|---|---|
| `:code-insight:completion:impl-cfir:test` | 27（+1） | **0** | 11:17:43 |
| `:analysis:analysis-api-cfir:test` | 1623 | **0** | 11:17:10 |
| `:code-insight:completion:impl-shared:test` | 37 | **0** | 11:18:33 |
| `:analysis:low-level-api-cfir:test` | 136 | 4（既有 CJMP 基线） | 11:20:30 |

### 遗留

隐式 receiver 候选由 9.2 的成员区段承担，本项不重复实现。

## 缺陷 D-3 取证：根因比原记录更具体，但修法不能删 require（2026-10-05）

任务 6.8。本轮**未修复**，产出的是定位结果与一条被否证的假设。

### 复现

把 `AbstractDanglingFileCollectDiagnosticsTest.prepareCjFile` 改成 Kotlin 形态
（只在 `IGNORE_SELF_MODE` 下设置 `originalFile`）后重跑生成套件：

```
7 个 DanglingFileCollectDiagnosticsTestGenerated 类 × 每类 3 失败 = 21 个失败
```

与 `implementation-log.md` 早前记载的「21 个生成用例失败」数字一致，复现成立。

### 真实根因（原记录写错了）

早前记录称根因是 `CfirCacheWithInvalidation.getNotNullValueForNotNullContext`
抛 Failed requirement（缓存不一致）。**实测不是**。真实堆栈：

```
DeclarationCheckersDiagnosticComponent.visitFile:375
  → LLCangJieSourceSymbolProvider.computeClassLikeSymbolByClassId:177
Caused by: java.lang.IllegalArgumentException: Failed requirement.
```

第 177 行是：

```kotlin
require(context == null || context.isPhysical)
```

即**非物理的 `CjClassLikeDeclaration` 被当作 context 传进了 class-like 符号缓存**，
于是解析抛错，整个文件的诊断收集中断（一条都收不上）。

### 被否证的假设（重要）

第一版修法是把那句 `require` 改成「忽略非物理 context、退回按 ClassId 查」。
失败数从 21 降到 14（每类修好 1 个），看似有效——**但这个改法是错的**：

Kotlin 参考实现 `analysis/low-level-api-fir/.../LLKotlinSourceSymbolProvider.kt:184`
有一模一样的 `require(context == null || context.isPhysical)`，且其调用点
`getClassLikeSymbolByClassId(classId, classLikeDeclaration)`（同文件 `:122`）
**直接把声明当 CONTEXT 传下去**。也就是说 Kotlin 依赖「调用方永远不传非物理声明」，
这个 `require` 是刻意的不变量断言。删掉它等于掩盖真实的调用方违约，
并让另两个失败（`Unknown diagnostic type CjOffsetsOnlyDiagnosticWithParameters1`）
继续以别的面貌存在。**两处改动已全部回退。**

（对照：`external/kotlin` 在本工作树里是空的，在**主检出**里是完整的。读参考实现
要走主检出，不能因为工作树为空就下「无法查证」的结论。）

### 下一步该查什么

不是 `computeClassLikeSymbolByClassId`，而是**谁**把非物理声明传了进去。
需要拿到完整调用链：在 `computeClassLikeSymbolByClassId` 的 `require` 处临时打印
`context.containingFile` / `context.copyOrigin` / 上一级 checker，
定位是哪一个 declaration checker 在 dangling copy 上按 PSI 查 class-like 符号。
定位到调用方后，按 Kotlin 的形状修**调用方**（不传非物理声明，或先 unwrap 到原件），
保留 `require`。

### 未覆盖

- 任务 6.8 保持未勾选。
- 15.1 分支 B 仍被 D-3 卡住；分支 A（平台补全进程）不受此影响。

### 续：找到真正的移植缺口 `KtFile.contextModule`（提交 `1fe72f9cb`）

对比 Kotlin 夹具全文时发现 Cangjie 移植**漏了一整步**：

```kotlin
// Kotlin: AbstractDanglingFileCollectDiagnosticsTest.kt:34-46
val contextModule = KotlinProjectStructureProvider.getModule(ktFile.project, ktFile, useSiteModule = null)
val fakeFile = ktPsiFactory.createFile("fake.kt", ktFile.text).apply {
    this.contextModule = contextModule          // ← Cangjie 移植完全没有这一步
    if (IGNORE_SELF_MODE in directives) { originalFile = ktFile }
}
```

Cangjie 侧 `CjFile` **没有** `contextModule` 这个能力（`CaDanglingFileModule.contextModule`
是另一个概念）。没有它，`createFile` 造出的副本既无 `copyOrigin` 也无 `elementContext`，
`computeContextModule` 落到 `getNotUnderContentRootModule(project)`——不是调用方要的上下文。

已按 Kotlin `KtFile.contextModule`（`analysis/analysis-api/.../danglingFiles.kt:58`）逐条对齐补上：
user-data 承载、代码片段不可设置、非内存文件不可设置；`computeContextModule` 最优先读它。

**但补上之后 21 个用例仍失败**，非物理声明照样被当作 context 传进 class-like 符号缓存。
测试夹具的 `originalFile` 仍保持「总是设置」，夹具的 Kotlin 对齐留待真正修好 D-3 时再做。

### D-3 现状（未修复，但边界已划清）

| 已确定 | 待做 |
|---|---|
| 21 个失败可复现（7 类 × 3） | 定位**哪个 declaration checker** 把非物理声明当 context 传入 |
| 根因在 `LLCangJieSourceSymbolProvider.kt:177` 的 `require` | 按 Kotlin 形状修**调用方**（不传非物理声明，或先 unwrap 到原件），保留 `require` |
| 该 `require` 与 Kotlin `:184` **完全一致**，是刻意的不变量断言 | `Unknown diagnostic type CjOffsetsOnlyDiagnosticWithParameters1` 这条独立失败另查 |
| 已补 `CjFile.contextModule`（真实缺失能力） | |

已否证的两条路（勿再走）：
1. 删 `require`（失败 21→14，但掩盖调用方违约，偏离参考实现）；
2. 只补 `contextModule`（不减少失败）。

### 续：拿到完整调用链，并逐项排除与 Kotlin 的分歧（2026-10-05 晚）

在 `require` 处打印调用栈，拿到确切的调用方（此前只知 checker 层的表象）：

```
LLNameConflictsTracker.getClassifierRedeclarations:55      ← 调用方在这里
  → UtilsKt.getAllClassLikeSymbolsByClassIdOrSingle:70
    → LLCangJieSourceSymbolProvider.getAllClassLikeSymbolsByClassId:166
      → getClassLikeSymbolByPsi:421
        → LLPsiAwareClassLikeSymbolCache.getSymbolByPsi:118
          → getSymbolByClassId:74        ← 非物理声明在此被当作 CONTEXT 传入
            → getNotNullValueForNotNullContext → computeClassLikeSymbolByClassId:177 → require 抛错
```

即：**名字冲突追踪器**（`CfirConflictsDeclarationChecker.checkFile` → `collectTopLevel`
→ `getClassifierRedeclarations`）要为顶层类查重声明，路径上把副本里的非物理声明
直接送进了 class-like 符号缓存。

### 已逐项核对、确认与 Kotlin **完全一致**（分歧不在这里）

| 项 | Kotlin | Cangjie |
|---|---|---|
| `require(context == null \|\| context.isPhysical)` | `LLKotlinSourceSymbolProvider.kt:184` | `LLCangJieSourceSymbolProvider.kt:177` | 
| 调用点把声明当 CONTEXT 传下 | `:122` | `:126` |
| provider 是否 `LLMultiClassLikeSymbolProvider` | 是（`:74`） | 是 |
| `getAllClassLikeSymbolsByClassId` 实现 | 逐字相同 | 同 |
| `getAllClassLikeSymbolsByClassIdOrSingle` 实现 | `utils.kt:70-74` | `utils.kt:69-73` 同 |
| 声明提供者构造 | `CompositeDeclarationProvider.create(..., factory(searchScope), ...)` | 同 |
| 游离模块 `baseContentScope` | `files.map { it.viewProvider.virtualFile }` | 同（`:114-117`） |
| `LLNameConflictsTracker.getClassifierRedeclarations` | `symbolProvider.getAllClassLikeSymbolsByClassIdOrSingle(classId).takeIf { it.size >= 2 }` | 同 |

**结论**：整条路径的结构与 Kotlin 一致，分歧在更细的一处（怀疑是
`CfirSession` 为游离模块构造 declaration provider 时的 `searchScope` 究竟指向
上下文模块还是副本自身——Kotlin 侧由 `KaDanglingFileModuleImpl` 继承上下文模块的
内容范围，Cangjie 侧尚未逐行核对 `LLCfirSession` / 声明提供者工厂的接线）。

### 下一步（不必重走以上比对）

1. 打印 `CfirSession` 的 `searchScope` 与 `LLNameConflictsTracker` 里
   `declarationProvider` 的实际来源，确认它对游离模块返回的是**上下文模块**的声明
   （物理）还是**副本自身**的声明（非物理）。若是后者，与 Kotlin 的差异就在这里。
2. 修**调用方**：`LLNameConflictsTracker.getClassifierRedeclarations` 在拿到非物理
   声明时，先 unwrap 到原件（仓颉已有 `unwrapCopy`）或直接按 ClassId 查询，
   **保留** `computeClassLikeSymbolByClassId` 的 `require`。

### 续：第十处比对也是一致，留下一个可验证的假设（2026-10-05 末）

`createScopedDeclarationProviderForFiles` 两边**逐字相同**，同样用 `file.virtualFile`、
同样「virtualFile 为 null 即视为在范围内」：

```kotlin
// Kotlin: LLFirAbstractSessionFactory.kt:768-789   Cangjie: LLCfirAbstractSessionFactory.kt:818-830
val virtualFile = file.virtualFile
if (virtualFile == null || scope.contains(virtualFile)) { add(...FileBasedDeclarationProvider(file)) }
```

所以「声明提供者建在副本自身上」这一点两边一致，不是分歧源。

### 剩下的唯一可验证假设：主缓存是否已被上下文模块预热

Kotlin 的 `LLPsiAwareClassLikeSymbolCache.getSymbolByPsi`（`:82-94`）在真正调用
compute 之前先走一次缓存快路径：

```kotlin
getCachedSymbolByPsi(classId, declaration)?.let { return it }   // ← 命中就不碰 compute
```

`getCachedSymbolByPsi` = `getCachedSymbolByClassId(classId)?.takeIf { it.hasPsi(declaration) }`
—— **只要该 ClassId 已在 `mainCache` 里，就不会用非物理 context 去调
`computeClassLikeSymbolByClassId`，`require` 自然不触发。**

Cangjie 的 `LLPsiAwareClassLikeSymbolCache` 是同一形状（`getSymbolByPsi` 在 `:118`
先调 `getSymbolByClassId`）。因此剩下的假设是：

> **游离文件的 LL 会话没有共享 / 复用上下文模块的 `classLikeCache`**，
> 于是 `sample/diagnostics/Printable` 这类 ClassId 在查重时是「冷的」，
> 必然进入 compute 并撞上非物理 context。

验证方法（下轮做，成本低）：
1. 在 `computeClassLikeSymbolByClassId` 的 `require` 之前打印
   `classLikeCache.getCachedSymbolByClassId(classId)`，确认失败场景下它确实是 `null`；
2. 对照**非** dangling 的同一 ClassId 查询，看是否命中缓存；
3. 若假设成立，修法在会话/缓存复用侧（让游离会话复用上下文模块的符号提供器缓存），
   而不是在调用方或 `require` 上动手。

### 状态

D-3 **仍未修复**；已排除 10 处与 Kotlin 的表面一致点；临时改动全部回退，
基线 analysis-api-cfir 1623 用例 0 失败。

## 类型位置补全递归（新缺陷，提交 `698b5886f`）

任务 9.5 测试侧推进时撞出来的独立缺陷。

### 现象与更正

`implementation-log.md`「9.5」节把递归记为：**「在声明名位置取导入作用域会与正在解析的
声明互相递归，实测触发 StackOverflowError」**。

**实测不准确**。声明名位置确实崩，但**类型位置也崩**：

```
CfirKeywordCompletionSectionTest.typeSuggestionsAppearOnlyAtTypePosition
  → java.lang.StackOverflowError
```

类型位置是用户最常补全的位置（`let x: Ty<pe>`、返回值位置等），该路径目前**完全不可用**。

### 递归点

栈里反复出现同一行（去重后只剩一帧）：

```
CaCfirCompositeScope.classifiers(CaCfirCompositeScope.kt)   ← collectNamespaced { delegate -> delegate.classifiers(names) }
```

即某个 `delegates` 成员的 `classifiers` 又回到了本组合。

### 否证的修法（勿再走）

在 `CaCfirCompositeScope` 构造时：
1. 展平嵌套的组合作用域；
2. 用 `IdentityHashMap` 在展开期防重入。

**两种都无效**，说明环不在组合的**构造期**，而在**查询路径**上。该改动已回退。

### 下一步方向

怀疑某个 `CaScope` 实现的 `classifiers` 内部又组装出一个**包含本组合**的视图
（例如某个 scope 的实现里调用了 `asCompositeScope` / `compositeScope`，而它的输入
包含了外层组合）。应打印 `delegates` 各元素的类名与 `identityHashCode`，
看哪两个互相持有。

### 测试处置

`typeSuggestionsAppearOnlyAtTypePosition` **只断言成立的缺席侧**（非类型位置不得出现
类型名建议），正面侧不写成通过的用例——按本变更纪律，不把「断言当前会崩」写成通过的测试。
KDoc 里写明正面侧当前崩溃及递归点。
