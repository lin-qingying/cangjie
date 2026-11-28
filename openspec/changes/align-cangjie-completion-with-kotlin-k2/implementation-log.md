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

---

## 类型位置补全递归：根因与修复（更正上一节）

上一节的「下一步方向」已被证伪。递归点不是「某个 scope 内部又组装出包含本组合的视图」，
而是 `CaCfirCompositeScope` 的**名称过滤重载自递归**：

```kotlin
// 错：lambda (Name) -> Boolean 精确匹配 nameFilter 形参，转发给了自己
override fun classifiers(nameFilter: (Name) -> Boolean) = classifiers { it.let(nameFilter) }
override fun callables(nameFilter: (Name) -> Boolean) = callables { it.let(nameFilter) }
```

正确写法与 `CaCfirBasedScope` 同形，先把名称集合算出来再委托 `vararg names` 重载：

```kotlin
override fun classifiers(nameFilter: (Name) -> Boolean) = classifiers(getPossibleClassifierNames().filter(nameFilter))
override fun callables(nameFilter: (Name) -> Boolean) = callables(getPossibleCallableNames().filter(nameFilter))
```

修复见 `39cfaa3c6`。修复后 `typeSuggestionsAppearOnlyAtTypePosition` 的**正面侧成立**
（类型位置可见 `Holder`、类别 `TOP_LEVEL`），该用例的两个 KDoc 也已更正——它们此前写着
「声明标识符上取导入作用域会互相递归」，那是错判。

**教训**：读栈帧定位递归时，行号必须对着**当前**文件读。此前两次结论（「环在
`classifiers(names)`」「环在构造器」）都是拿旧行号对已改动的文件，方向被带偏。

---

## 持久化符号索引链路（6.5 / 6.6③ / 6.10 / 9.10）

### 契约形状的三次修正

最初把 `CaSymbolIndexQuery` 写成「工厂绑定 scope + 查询再传 scope + 返回 `CaSymbol`」，
逐条被证伪：

1. **工厂绑定范围却又要求每次查询传范围** —— 同一条约束两处表达，必有一处漂移。
   改为范围只在工厂绑定。
2. **返回 `CaSymbol` 迫使契约持有 `CaSession`** —— 会话与符号都是**分析生命周期**对象，
   而索引是可替换、可跨会话的实现（IDE 侧持久）。把它们写进契约等于让持久索引持有会话。
   改为返回 `CaSymbolIndexEntry`（包 + 短名 + 种类 + 枚举构造器的 `ownerClassId`），
   符号恢复留给 Analysis 层。
3. **符号恢复放在平台实现里** —— 会让三个索引实现各写一遍「名称→符号」，正是 A8 要避免的。
   改为：恢复只有一处，在 `CaCfirSymbolProvider.resolveIndexEntry`。

前缀分级（≤3 字符且非重复只做起始匹配）**不写成实现方义务**，而是 `CaSymbolIndexNameFilter`
里的契约代码：三个实现必须返回同一名称集合，各自实现匹配必然漂移。

### 补全层不能直接依赖平台 SPI

`impl-cfir` 只依赖 `analysis-api`，不依赖 `analysis-api-platform-interface`（刻意的分层：
补全层消费 Analysis 公共面，不消费平台 SPI）。因此索引入口加在
`CaSymbolProvider.getDeclarationsByNamePrefix`，而不是让区段去 `project.getService(...)`。

### 摘要缺陷：枚举构造器不在成员名集合里

`CaStubTreeSummaryExtractor.collectClassMemberNamesFromChildren` 只收集
callable / typealias / 嵌套类型，**漏了 `CangJieEnumConstructorStub`**。
后果：枚举构造器的名字根本不进快照，按名查询枚举值恒为空。
已补该分支（`is CangJieEnumConstructorStub -> memberNames += name`）。

### 被否证的做法：用三态候选判定

第一版索引区段用 `CaCompletionCandidateChecker.checkCompletionCandidate` 做可达性判定。
两个问题：①设计 A4 明确「禁止以现有 DIRECT/REQUIRES_IMPORT/HIDDEN 结果替代适用性检查」；
②该路径在**导入绑定尚未产出的文件**上直接抛
`IllegalStateException: File import bindings are missing after IMPORTS phase`（夹具复现）。
改为按符号自身包路径构造 `AddImport` 计划，与作用域路径的重合候选交由
`distinctByIdentity` 合并。

### 顺带修掉的去重前提缺陷

`classifierSnapshot` 的身份句柄原先只取短名：跨包同名类型会得到**同一个去重键**，
索引段与顶层段各给一份时会被错误合并成一个。改为 `classifierIdentityKey`
（class-like 取 `classId` 字符串，局部类型/类型参数回退短名），
`CfirCompletionRecoveryService` 的类型定位同步改用它——否则复查会按短名认领到另一个包的同名类型。

### 索引只做名称收窄，不负责重载身份

`CaStubFileSummary` 的顶层名称是 `Set<Name>`，**名称层无法区分重载**。
因此「同名不同参数必须各自保留」的保证在**恢复阶段**：`getTopLevelCallableSymbols(pkg, name)`
返回全部重载，`declaredMemberScope.callables(listOf(name))` 返回全部同名枚举构造器。
测试相应地分层断言：standalone 侧断言条目（同名重载在名称层就是一条），
补全侧断言恢复后的候选（两个重载各自带签名、去重键互不相同）。

### 测试宿主必须补注册服务

`CaCfirStandaloneAnalysisApiTestConfigurator` 不加载整份 standalone 描述符，
索引服务在测试宿主里**不存在**。缺服务时区段返回空集合，表现成「缺席断言通过」——
正好掩盖未注册。因此加了 `IndexSectionTestServiceRegistrar` 按生产实现显式注册。

---

## 既有缺陷（本次未修，仅记录）

### API surface 基线早已过期

`analysis-api` 与 `analysis-api-platform-interface` 的 `api/*.txt` 在改动前就与源码不一致
（`CaScopeContext`、`CaMemberScopeQuery`、`CaCompletionExtensionCandidateChecker`、
`analyzeCopy`、`CaScopeProvider.typeParameters` 等此前从未并入；平台侧另有两处）。
也就是说这两模块的 `checkApiSurfaceDump` **在本次改动前就是红的**。
本次重新生成会把这些既有漂移一并吸收，`checkApiSurfaceDump` 随后通过。

### dump 提取器把「私有构造 + 公开类」当成非公开

`analysis-api-platform-interface` 的 `CangJieFileBasedDeclarationProvider`（工厂形态，
构造函数 private、类本身 public）整段从基线消失：提取器的
`nonPublicModifierRegex` 命中 `private` 就丢掉整个声明。属既有工具缺陷，本变更不修。

### 同一提取器缺陷的第二个实例

新增的 `CaStandaloneSymbolIndexQueryService(private val project: Project)`
**没有**出现在 `analysis-api-standalone` 的 API 基线里：类的构造参数带 `private`，
提取器的 `nonPublicModifierRegex` 命中后丢掉整个类。基线因此是**不完整**的
（`checkApiSurfaceDump` 通过，但那不等于 dump 覆盖了全部公开声明）。

与 `CangJieFileBasedDeclarationProvider` 同一根因。三处（平台接口、standalone、
以及任何「公开类 + 私有构造参数」的声明）都受它影响。**本变更不修提取器**：
修好它会让多个模块的基线一次性多出此前被静默隐藏的声明，那是一次独立的基线迁移，
应单独评估，不夹带在补全对齐里。

---

## 索引内容补齐：`.cjo` 名字通道与发现覆盖（6.6④ / 6.7）

### 两条通道必须在**名称层**合并

`.cjo` 名字视图并入后，同一个名字会被源码索引与二进制名字视图各报一次。
第一版实现让两条通道各自解析符号，结果同一个声明变成两个候选——
回归直接打出来：实例化重载测试从 2 个候选变成 4 个，`single {}` 抛
「Collection contains more than one matching element」。

反过来也不能按**解析结果**去重：同名重载的 `callableId` 完全相同，
按它去重会把合法重载合并掉，正是 A8 禁止的事。

落点：按「包 + 短名 + 种类 + 所属类型」在**名称**层合并，解析只做一次。
`DiscoveryEntry` 因此只在分析层存在，不上平台契约——
二进制名字视图不暴露枚举构造器（它只给顶层名字），所以它的 `ownerClassId` 恒空。

### 夹具里写了 Kotlin 的 `typealias`

Cangjie 的类型别名关键字是 **`type`**，不是 `typealias`。cjc 1.1.3 实测：
`public typealias Alias = Widget` → `expected declaration, found 'typealias'`；
`public type Alias = Widget` → 只报预期的 `'main' is missing`。
（仓库内部仍用 `CjTypeAlias` / `CangJieTypeAliasStub` 命名，那是实现名不是语法。）

### 失效与取消的可验证性

- **失效**：测试宿主不加载整份 standalone 描述符，`CaStandalonePlatformState` 不存在；
  补注册它又会替换掉框架已装好的模块图（实测把已经通过的两个用例一起打崩）。
  改为给 `CaStandaloneSymbolIndexQueryService` 增加一个**默认取平台修改计数**的
  `versionProvider` 参数——缓存键取错的表现是静默的（不报错，只是候选永不更新），
  这条契约值得直接验证，而不是绕开。
- **取消**：`ProgressManager.checkCanceled()` 已在查询与构建循环里，测试用已取消的
  `ProgressIndicator` 断言抛 `ProcessCanceledException`——吞掉取消会让界面把
  「请求被取消」显示成「没有候选」。

### 服务构造器不能带默认参数

把版本来源做成构造参数（`(Project, () -> Long = ...)`）直接打崩了四个索引区段测试：

```
PicoIntrospectionException: CaStandaloneSymbolIndexQueryService has unsatisfied dependency:
interface kotlin.jvm.functions.Function0
```

平台按 `(Project)` 单参构造实例化 `projectService`；带默认值的双参构造器让 Pico 去解析
`Function0` 依赖并失败。Kotlin 的默认参数在这里不是「向后兼容的便利」，
而是**改变了容器可见的构造签名**。改为单参构造 + 模块内可替换的 `versionProvider` 属性。

---

## 6.9 D-4 复核（2026-10-07）

重跑锁定用例并**按 §6 纪律核对 XML 实际执行数**（不只看 `BUILD SUCCESSFUL`）：

```bash
./gradlew :analysis:analysis-api-cfir:test \
  --tests org.cangnova.cangjie.analysis.api.cfir.test.cases.generated.cases.components.scopeProvider.CfirIdeNormalAnalysisSourceModuleMemberScopeTestGenerated \
  --tests org.cangnova.cangjie.analysis.api.cfir.test.cases.generated.cases.components.scopeProvider.CfirIdeNormalAnalysisSourceModuleDeclaredMemberScopeTestGenerated \
  --rerun
```

结果：两个类各 `tests="2" failures="0"`，夹具 `memberScopeQueries.cj` 中
`DECLARED_MEMBER_SCOPE_AVAILABLE_NAME` / `DECLARED_MEMBER_SCOPE_CALLABLE` 的
`field` 与 `state` 指令齐备。D-4 的修复仍然生效。

`handover.md` §5 第 4 条「根因未最终定位」已回写为已修，并补记两处真实落点：
`CfirClassUseSiteMemberScope`（不是四层声明作用域）与字段稳定指针缺失。

---

## 10.6 插入：括号形态以文档现状为准（2026-10-07）

### 缺陷

`performCangjieInsertion` 原先只看分析期结论：`isFollowedByOpeningParen` 命中时
**只是不写括号**，caret 却留在左括号之前。用户插进 `alpha|()` 后 caret 停在 `(`
前面，接着敲的实参落在括号外。既有用例还把这条错行为断言成了正确行为
（`assertEquals(5, caret, "已有括号时 caret 停在插入点")`）。

### 改法

分析期冻结的是**意图**（`insertParenthesis` + `caretPolicy`），
括号的**实际形态**在写入时从文本判断，四态：

| 文档现状 | 行为 |
|---|---|
| 后面没有左括号 | 补一对 `()`，caret 进括号内 |
| 已闭合、括号内有非空白内容 | 一个字符都不写，caret 停在右括号之前 |
| 已闭合、括号内只有空白 | 不写，caret 进左括号之后（不跳过空格） |
| 有左括号但未闭合 | 不写，caret 进左括号之后 |

未闭合那条是新发现的：原实现会补出 `foo(()`。那是一段**无法通过增量输入恢复**的
文本——用户既没法删掉多余的 `(` 而不影响自己刚写的那个，也看不出多出来的是哪个。

只按非空白字符判断「括号内有没有内容」：`foo( )` 与 `foo()` 一样是「还没写实参」。

### 删除 `InsertionTrigger`

契约里的 `InsertionTrigger`（ENTER / TAB / TYPING）**从未被任何代码引用**。
三种触发的差别由平台给出的替换区间（`startOffset`/`tailOffset`）与当时的文档状态
共同表达，插值阶段不需要也不应该再分叉。保留它等于对外承诺一组不存在的分别处理——
契约里声明了却没人读的枚举，比缺失更容易误导后来者以为已有覆盖。

---

## 10.2 语义复查接入选路径（2026-10-07）

### 发现的缺口

`CfirCompletionRecoveryService.recover` 实现了、有 5 个用例，但**没有任何生产调用点**。
契约里的 `InsertionValidity.DanglingCandidate`（"候选身份已失效：重新解析后目标不再存在"）
同样是早就预留、无人生产的枚举分支。也就是说「失效候选不得写文档」此前只对
**文档版本**失效成立，对**语义**失效不成立。

`PsiImportPlanApplier.apply(file, plans)` 的 `plans` 由调用方计算——这个「调用方」
正是复查该在的位置：写入前重算导入计划 + 确认目标还在。

### 分工

| 时机 | 动作 | 为什么 |
|---|---|---|
| 选择期（读动作） | `recover(useSite, identity)` → 回挂结论 | 插入阶段在写事务里，那里不能跑分析 |
| 写入期（写事务） | 只读结论 | 不能分析，但必须知道目标还在不在 |

### 三态与降级

`RevalidationVerdict` = 未复查 / 已复查（带复算后的导入计划）/ 目标失效。

**复查失败降级为「未复查」而不是「失效」**是关键决定：EDT 上不允许分析、会话已过期、
用户取消，都是**环境限制**，与「候选过期」是两回事。把它们判成失效，用户在正常环境里
会选什么都插不进去，而且看不出原因。只有复查**确实回答**目标不存在时才拒绝写入。

`revalidateSelection` 幂等：已复查的元素不重查——用户在弹窗里来回移动选中项是常态，
每次都重新进入分析会把这个动作变成分析风暴。

### 本模块装不了监听器

第一版实现直接在 contributor 里 `LookupManager.getInstance(project).addLookupListener(...)`，
编译失败：`impl-shared` 的 `compileOnly` 只有 `intellij-core`、`analysis`、`indexing`，
**不含平台的 Lookup 库**。这不是配置疏漏，而是 B1 的分工——主仓承载共享语言实现，
Lookup 接线属于宿主。

因此落地形态是：主仓提供**可测的复查逻辑**（`revalidateSelection`，不依赖 Lookup API）
与**宿主接缝**（`recoveryService` + `revalidateSelection(item, useSite)`），
宿主在自己的 `currentItemChanged` 里调用。宿主未调用时退化为「不做语义复查」，
文档版本与区间校验仍然生效——是明确的降级，不是静默失效。

---

## 10.5 使用点目标复验：导入可达性按结构判定（2026-10-07）

### 缺陷

`CfirCompletionRecoveryService.importPlanFor` 用「整行文本 == 目标包路径」判断文件里
是否已经有等价的 import。这条判断两个方向都错，两个方向都用 cjc 1.1.3 实测确认过：

**漏判**（往已经能用的文件里重复写 import）。仓颉里同一个目标有三种等价写法，
都能直接引用 `c()`：

```cangjie
import probe.dep          // 包级
import probe.dep.c        // 声明级
import probe.dep.*        // 星号
```

实测：`import probe.dep.c` + `c()` 编译通过；`import probe.dep` + `c()` 也编译通过；
`import std.collection.*` 解析通过。只比整行文本时，后两种一律识别不到。

**误判**：`import a.b` 既可指包 `a.b`，也可指包 `a` 中的声明 `b`。语法同形，
仅凭文本无法区分，按「至少覆盖其一」判定即可——重复写一行 import 才是要避免的。

### 改法

走 PSI 的 import 项按结构判定：星号导入按包名比对，包级与声明级按完整名比对。
`CjImportItem.toImportPath()` 由 private 放开为公开，作为「PSI import 项 → ImportPath」
的唯一映射复用，避免两处各写一份解析（该函数的 KDoc 本来就写着"走 PSI 而非字符串反解"）。

顺手修掉该映射里的 `importedFqName?.takeIf { !isAllUnder } ?: return null`：
`isAllUnder` 是 `ImportPath` 自带的字段，这个 guard 恰好把它存在的意义丢掉了，
使「文件里已经 `import a.b.*`」这一事实在路径集合里完全消失。
对导入应用器无行为影响（星号导入的 `importedName` 为 null，本就不参与 covers/conflict 判定），
但对可达性判定是必需的。

---

## 10.3 权重：类别维度与位段验算（2026-10-07）

设计 §7.3 的覆盖清单列了七维。本轮补上**类别**后为六维，
唯一未做的是**参数匹配**。

### 类别只压低附属条目

`categoryRank` 只把 `DOCUMENTATION` 压到真实候选之下，**不**给各类符号之间定次序：
同一位置上的候选已经被位置分类限制在同种类里（类型位置只出类型、成员位置只出成员），
跨种类次序没有可依据的规范。编一套出来只会让排序看起来有道理却无法解释。

`CompletionItemKind` 目前**没有**「声明生成建议」——那个值在 `CompletionIdentityKind` 里。
将来补全入口引入该类别时，其权重必须落在 `DOCUMENTATION` 一档或更低：
「要不要新建一个声明」是兜底方案，不是命中。这条写进了 KDoc，免得后来者随手给个中间值。

### 位段必须逐维验算，不能凭感觉

原实现的位段是 `1e6 / 1e3 / ±100 / -50 / 0..49`，其中同级声明次序**无界**：
一个声明靠后的候选足以越过「是否需要导入」维度。改为显式限幅后，
每一维的步长必须**严格大于其下各维的全幅跨度**：

| 维度 | 步长 | 其下全幅跨度 | 是否满足 |
|---|---|---|---|
| 来源 | 1e6 | 1e3×20 量级 | ✓ |
| 导入成本 | 1e3 | 200+50+10+9 = 269 | ✓ |
| 期望类型 | 200 | 50+10+9 = 69 | ✓ |
| 类别 | 50 | 10+9 = 19 | ✓ |
| 弃用 | 10 | 9 | ✓ |
| 同级次序 | 0..9 | — | — |

这类错误排序是**静默**的：不报错，只是候选顺序看起来没道理。因此专门写了
`categoryNeverCrossesHigherDimensions`，用 `Int.MAX_VALUE`/`Int.MIN_VALUE`
把低位维度逼到极值再断言高位维度仍然压得住。

### 参数匹配为什么不臆造

设计全文只在 §7.3 的覆盖清单里提过一次「参数匹配」，没有口径；仓内也没有
「参数签名 / 期望参数」契约。真正的适用性检查属于尚未实现的 A4
（`CaCompletionExtensionCandidateChecker` 只覆盖 extend 适用性），而 A4 又明确
禁止用三态旧接口替代。因此保持现状：`ExpectedTypeMatch.UNKNOWN` 时不产生任何影响，
这就是设计要求的「未知时明确降级」。

---

## 7.5（缺陷 D-6）：前提核查——参考实现里没有桥接生成器（2026-10-07）

任务原文要求「定位并跑真实 context 桥接生成器，消除 `CaScopeProvider` 新增成员的手改桥接；
验收：生成器产出桥接，且手改桥接被禁止」。

核查结果：**这个生成器不存在**。

### 证据

1. `external/kotlin/analysis/` 下只有两个生成器模块：`analysis-api-fir-generator` 与
   `analysis-tools/deprecated-k1-frontend-internals-for-ide-generator`；
   对整棵 Kotlin 检出按 `bridgesgenerator` / `ContextBridgeGenerator` / `generateBridges`
   搜索无命中。
2. Kotlin 的桥接确实带 `// Auto-generated bridge. DO NOT EDIT MANUALLY!` 注释，
   **但它们就写在组件接口文件里**（如 `KaScopeProvider.kt`），与我们的形态一致。
   该注释是**约定**，不是 Gradle 代码生成产物的标记。
3. 桥接是**选择性**的，不是「每个成员一条」：`KaScopeProvider.kt` 有 16 条桥接，
   而其中的成员声明远多于 16。生成器无法在不知道「哪些成员需要桥接」的前提下产出它们——
   这个判断本身就是人写的设计决定。

### 我们这边的现状

`CaScopeProvider.kt` 9 条、`CaVisibilityChecker.kt` 3 条，同样带
「自动生成的 context 桥接。请勿手工修改。」注释，同样写在组件文件内。

### 结论

按原样验收「生成器产出桥接」会要求发明一个参考实现不存在的工具，并且要把它塞进
`analysis-api` 的编译回路——生成器需要已编译的 `analysis-api` 做反射输入，
而 `analysis-api` 又需要生成结果才能编译，这是一条真实的循环依赖。
**不为满足任务措辞而造这个工具。**

D-6 的真实风险是「加了成员忘了写桥接」，可机械化的部分不是生成而是**校验**：
把「哪些成员需要桥接」写成显式清单，再用测试断言清单里的每一条都有对应桥接。
这条留待与 7.3（共享 Analysis component 基座的新契约测试）一并处理，
而不是在这里临时造一个半成品生成器。

---

## 7.4 受影响 Analysis/LL 窄集执行记录（2026-10-07）

```bash
./gradlew :analysis:analysis-api:test :analysis:analysis-api-cfir:test \
  :analysis:analysis-api-standalone:test :analysis:analysis-api-platform-interface:test \
  :analysis:stubs:test :analysis:low-level-api-cfir:test
```

| 模块 | tests | failures | errors |
|---|---|---|---|
| `analysis-api` | — | — | — （纯契约模块，无 test source set） |
| `analysis-api-platform-interface` | — | — | — （同上） |
| `analysis-api-cfir` | 1623 | 0 | 0 |
| `analysis-api-standalone` | 712 | 0 | 0 |
| `stubs` | 8 | 0 | 0 |
| `low-level-api-cfir` | 136 | **4** | 0 |

补充（同一源码状态下的补全侧窄集）：`code-insight:completion:impl-shared` 47/47、
`impl-cfir` 46/46，均 0 失败。

`low-level-api-cfir` 的 4 条失败全部是
`CfirCjmpCallerFirstDiagnosticsTest`：`defaultArgumentCallerFirst`、
`defaultArgumentInferredCallerFirst`、`shadowedCommonCallerFirst`、
`shadowedCommonInferredCallerFirst`。这是**已记录的既有基线**（见
`cangjie-analysis-verified-defects` 记忆与 6.8 的取证），与本次改动的文件无交集——
本次未触碰 CJMP 调用者优先相关的解析或诊断链路。

「无 test source set」的两个模块不是漏跑：它们是纯契约模块，
其正确性由消费者（`analysis-api-cfir`、`analysis-api-standalone`）的用例覆盖。

---

## 8.9 命名与包名复核（设计 §3，2026-10-07）

**范围澄清**：设计里说的「42 个文件」指 **`code-insight/completion` 子树**，
不是整个 `code-insight/`——后者是多特性树（api / fixes / folding / formatting / completion …），
共 114 个 `.kt`，各自的包名策略不同，不属于本项范围。

### 复核结果（completion 子树）

| 项 | 结果 |
|---|---|
| `.kt` 总数 | 51（主源码 34 + 测试 17） |
| 包名 | 全部落在 `org.cangnova.cangjie.ide.completion.*`（12 个包），**零例外** |
| `Ca` 前缀类型声明 | **0**（顶层与缩进声明都检查了） |
| 文件名 = 主声明名 | 51 个文件中 5 个不满足，逐条判定见下 |

文件数从设计基线的 42 增到 51，是本变更新增所致
（`CfirEnumEntryCompletionSection`、`CfirIndexCompletionSection`、`RevalidationVerdict`、
`CfirInsertionIntegrationTest`、`IndexSectionTestServiceRegistrar`、`CompletionSectionTestSupport` 等）。

### 5 处文件名差异的判定

| 文件 | 实际内容 | 判定 |
|---|---|---|
| `CompletionFiltering.kt` | `CompletionExpectedTypeKind` + `CompletionExpectedType` + `CompletionFilter` | **合规**——设计 §3.2 原文就把它列为「同族辅助类型共居」的正面样板 |
| `CompletionRecovery.kt` | `InsertionValidity` + `RecoveredTarget` + `CompletionRecoveryService` | **合规**——同一契约的三个组成部分，单独存在没有意义 |
| `CfirSnapshotFactory.kt` | 只有顶层扩展函数，无类型声明 | **合规**——§3.2 的「文件名 = 主声明名」只约束**有主类型声明**的文件；该文件是快照工厂函数集合 |
| `LookupIcons.kt` | 只有 `iconOf` 等函数 | **合规**——同上，文件名表示领域 |
| `CompletionSectionTestSupport.kt` | 测试辅助扩展函数 | **合规**——测试源码，同上 |

§3.2 另有一条「纯函数集合用 `<area>Util.kt` / `<area>Utils.kt`」。
上面三个纯函数文件（`CfirSnapshotFactory` / `LookupIcons` / `CompletionSectionTestSupport`）
**未**采用该后缀：它们的名字描述的是**职责领域**而非「工具集」，
而该条规则针对的是泛用工具集合（PSI 侧 `psiUtil/` 那种）。
这一处是有意的取舍，登记为显式决策：**新增纯函数文件优先用职责名，
只有真正泛用的工具集合才用 `*Util(s)`**。

### 显式决策（本项要求登记的）

1. **包名统一采用 `org.cangnova.cangjie.ide.completion.*`**——实测零例外，不做迁移。
2. **`code-insight` 补全层不使用 `Ca` 前缀**——`Ca` 保留给 Analysis 公共契约
   （`CaSession`/`CaType`/`CaScope` 等跨层类型）。实测补全子树零 `Ca` 前缀类型声明。
3. 纯函数文件命名按上一节的取舍处理。

---

## 8.6 执行器的延迟阶段（2026-10-07）

执行器原先把所有区段按优先级一次跑完：索引区段要枚举源码倒排表与 `.cjo` 名字视图，
是管线里最慢的一步，却和词法候选同批执行——用户必须先等它算完才能看到本来立刻就能给的候选。

两阶段划分后：快速阶段（词法/成员/关键字/顶层/命名实参）先跑并立即送达批次；
延迟阶段（索引召回）随后跑，**进入前重新检查取消**。用户继续输入时，
前半段结果早已送达并可直接展示，再去算索引候选纯属浪费，而且会把一次被取消的请求
拖成一次慢请求。

**阶段划分 ≠ 延迟计时**：本层只保证「快速批次先于延迟批次」，
真正的延迟时长由宿主决定——它才知道用户是否还在输入。这条写进 KDoc，
免得后来者以为这里已经实现了计时。

收尾语义同时收紧：从「最后一个区段之后」改为「全部阶段完成后恰好一次」。
延迟阶段被取消时整个调用抛取消异常，由调用方区分「未完成」与「完成」。

### 两处测试侧约束

- `EmptyProgressIndicatorBase` 把 `checkCanceled()` 定成 **final**，无法覆写来模拟取消。
  改为只覆写 `isCanceled()`、由测试在快速阶段结束时置位——而执行器正是通过
  `checkCanceled()` 观察取消，这恰好验证了两者一致。
- `CjParsingTestCase` 的建文件入口是 `createPsiFile(name, text)`，不是 `parse(...)`。

---

## 8.1–8.4 复核与补齐（2026-10-07）

### 8.1 模块骨架

三个模块在 `settings.gradle.kts`（第 77–79 行）与 `docs/module-catalog.md`（第 53–55 行）
均已注册。目录现状：`contracts`（src）、`impl-shared`（src/test）、
`impl-cfir`（src/resources/test/testData）。

**没有 `testResources`**：它只在存在测试专用描述符时才有意义，
而设计要求「禁止把测试 plugin.xml 混入产品」——真正的测试描述符属于 8.7/12.4 的范围，
在没有这种需求时建空目录只会留下无法被 git 跟踪的空壳。**不为了凑目录清单而建空目录。**

### 8.2 接口层次

跨模块服务 `CangJieCompletionService`、策略接口 `CompletionFilter`、
内部 `CompletionSection`/`CompletionRunner`/`CompletionResultSink` 齐备；
生产描述符只注册**一个** `completion.contributor`，没有为任何内部贡献者加 EP
（`grep completion.weigher` 命中数为 0，各 section 由 `CfirCompletionService.sections`
的显式列表组织）。

### 8.3 参数包装

`PlatformCompletionRequest` 承载原件/副本两个位置、offset、prefix、invocationCount、
BASIC/SMART（`CompletionKind`）与替换区间；`documentStamp` 取**原始文件**的版本戳
（分析副本上的 dummy 修正与用户文档无关）。

**一处如实记录的歧义**：`originalPosition` 目前与 `completionPosition` 同值（都在副本里、
且是**修正后**的位置）。契约说它「触发本次补全的原始 PSI 位置」并补充「未修正时等于
completionPosition」，设计 B2 说「原件/副本」——两处措辞指向两种读法，而当前实现两者都不是。
它目前**没有消费者**（语义复查走自己的 use-site 参数，插入校验用 `documentStamp`），
因此不猜着改；第一个真正的消费者出现时按它需要的语义定，并用例锁定「两者必须可区分」。

### 8.4 dummy 与位置修正

修正覆盖：表达式/函数体/成员访问/命名实参位置换成 `cangjieCompletionPlaceholder()`；
类型位置保持裸标识符（替换成调用反而会把候选从类型变成值）；原始字符串内直接排除
（不插值）；泛型限定名后与字符串插值内同样替换。

**这套逻辑此前没有任何用例**，因为它唯一的入口依赖平台的 `CompletionParameters`。
新增 `provideFor(file, offset)` 后补齐 8 条用例。

**过程中挖出一个被静默吞掉的真实失败**：占位替换原先是
`runCatching { expression.replace(placeholder) }.getOrNull()`。去掉吞异常后立刻现形——
`IncorrectOperationException: Must not change PSI outside command or undo-transparent action`。
也就是说**占位替换是一次 PSI 写，必须在命令内执行**；平台补全入口本身处于补全命令内
所以从未暴露，而这条前置条件此前既不在文档里、也无法被测试发现。现已写进 `provideFor` 的
KDoc，测试按契约开命令执行。

**未做**：`typed-handler` 自动触发的协调（设计 §7.2「避免重复弹窗」）——
typed handler 属于宿主平台接线，不在本工作区。

### 顺带记两个 Kotlin 陷阱

1. `kotlin.test.assertEquals` 在期望值与被测值**都是字符串**时会选中 message-first 重载，
   位置参数会把断言消息当成被测值（失败信息里 expected 是期望值、was 却是断言之文本）。
   本变更里踩了两次，字符串断言一律改用命名参数。
2. `kotlin.test.assertNotNull` 在某些解析下会选中返回 `Unit` 的重载，
   使 `assertNotNull(x).foo` 报「Unresolved reference 'foo'」。用 `requireNotNull` 更稳。

---

## 11.4 CDoc 候选与「文档位置一直不可达」（2026-10-07）

### 实现

`CfirCDocCompletionSection` 处理 `/** … */` 内的三种形态（仓颉 CDoc 的主语写作
`@param [value]`，方括号里是链接而不是裸名字）：

| 位置 | 候选来源 | 插入文本 |
|---|---|---|
| `@` 之后 | `CDocKnownTag` 枚举 | 标签名，**不带 `@`** |
| `@param [` 内 | **所属声明的形参列表** | 参数名 |
| `@see [` 内 | 文件导入作用域 | 声明名 |

两处判据值得记：

- **所属声明**不能取位置分类给的 `declaration`：文档注释通常写在声明**之前**，
  此时它是文件的直接子节点，分类给不出 declaration。改用「第一条起点在这条注释之后的
  callable 声明」——比逐级爬 `nextSibling` 稳，爬链在遇到别的成员时很容易提前放弃。
- **已用标签不再给**：同一个标签写两遍没有意义，列出来只会让用户重复选中。

### 发现的更严重问题：`DOCUMENTATION` 位置种类从未被分类出来

实现时发现区段的守卫永远为假。探针实测：

```
commentClass=CDocImpl isCjElement=false
depth=0 PsiWhiteSpaceImpl isCj=false kind=null
depth=1 CDocImpl        isCj=false kind=null
depth=2 CjFile          isCj=true  kind=EXPRESSION
```

文档注释由 CDoc 词法产出 `CDocImpl`，**不是 `CjElement`**（`CDocElement` 与 `CjElement`
是两套接口）。因此注释内最近的 `CjElement` 祖先是整个文件，`classify` 只会给出
`EXPRESSION`——`DOCUMENTATION` 这个分支**从来没有被走到过**。

两条后果：
1. 注释内的候选此前走普通表达式路径，会给用户代码符号（在文档注释里写代码名字）；
2. 既有代码里所有对 `DOCUMENTATION` 的排除（`CfirKeywordCompletionSection`、
   `CfirExpectedTypeCompletionFilter`）都是**死分支**——不是「防御性编程」，而是从未生效。

### 修法（加性）

`CompletionRequest` 新增 `positionElement: PsiElement`，默认取 `completionPosition`，
承载光标处的**原始叶子元素**；位置形态分类改用它，分析位置仍用 `completionPosition`。
不改既有字段的类型，因此所有现有调用点无感。平台侧填入 `parameters.position`。

这条不是「为了过一个测试」的补丁：位置分类本来就应该看光标处**真实**的 PSI，
而不是看经过 dummy 修正、且可能根本不在仓颉 PSI 里的分析位置。

---

## 11.6 未导入 receiver 的成员恢复（2026-10-07）

### 让功能彻底静默失效的判据错误

触发条件最初写成 `resolved == null`。但**未解析引用的 `expressionType` 返回的是
`CaErrorType` 而不是 null**——于是恢复路径永远不会触发，成员候选恒为空，且不报任何错。
这正是本变更里反复出现的失败形态：**没有候选 ≠ 有错误**，这类缺陷只能靠用例发现。

定位过程记一笔：先验证了两个前提（索引能查到 `Widget`、receiver 确实是
`CjSimpleNameExpression`），都成立；再在恢复函数的每个提前返回点加临时诊断，
打印出 `all=[CaCfirClassSymbol:Widget]` 与 `ok pkg=recovery.dependency`——
恢复本身是好的，是它压根没被调用。

### 三条边界

| 约束 | 为什么 |
|---|---|
| **只认裸标识符**（类型检查，不看文本） | `a.b.c` 时 receiver 是限定表达式，直接放弃。任意深度的表达式链搜索不属于本恢复的范围，代价与收益完全不成比例 |
| **只认精确同名** | 索引接口按前缀召回，长前缀退化为**子串**匹配；不再按完全相等过滤一次，`Foo` 会把 `FooBar` 带进来 |
| **必须唯一** | 同名类型来自多个包时给不出确定的接收者类型。随便挑一个会把用户带到错误的成员集合上，比不给候选更难发现 |

预算是**结构性**而非超时的：三条约束把工作量钉死在「一次索引查询 + 一次类型解析」，
不存在需要额外设阈值的长尾。取消检查在恢复前做一次。

### 成员与导入必须一起做

只插入 `Widget.size` 而 `Widget` 仍不可解析，用户得到的是一段编译不过的代码。
恢复出的候选因此带上接收者所属包的 `AddImport` 计划，
与 10.5 的导入策略、10.7 的写入路径直接对接。

### 一条夹具事实

类型限定访问 `Widget.size` 走**静态**成员作用域，实例方法在那里找不到。
cjc 1.1.3 实测 `public static func` + `Widget.size()` 合法。
夹具最初写成实例方法，表现为「恢复成功但候选为空」，一度被误判为恢复失败。

---

## 11.3 match 模式候选（2026-10-07）

仓颉按主语类型的枚举构造器分派 `match`，用户在 `case` 后面敲的几乎总是其中之一，
而此刻他**没有任何接收者可写**——`Dot` 不像 `Shape.Dot` 那样有个类型名可以打出来。

候选来源与成员访问路径**完全同源**（都按 `CaEnumConstructorSymbol` 过滤），
区别只在「枚举从哪来」：成员访问从接收者，模式位置从 `CjMatchExpression.subjectExpression` 的类型。

### 位置判据按偏移划界

一个 case 子句里既有模式也有体（`case Dot => 1`），只有模式那一段是模式位置，
体里该给普通表达式候选。判据写成「位置起点早于体的起点」，**不是**「有没有解析出模式」——
用户正打到一半时模式往往还没成型，按结构判会漏掉最常见的时刻。

### 一条「看起来像存在」的 PSI 陷阱

`psi/.../CjMatchCondition.kt` 里的那个类**是被注释掉的占位**：

```kotlin
// abstract class CjMatchPattern(node: ASTNode) : CjElementImpl(node)
```

我按文件名与直觉先用了 `CjMatchCondition`，直接编译失败（Unresolved reference）。
可用的是 `CjMatchEntry`（case 子句）与 `CjMatchExpression.subjectExpression`（主语）。
**文件存在 ≠ 类型存在**——查 PSI 时要落到声明行，不能只看文件名。

---

## 11.5 分流：位置分类入口不统一（2026-10-07）

### 分流用例直接把问题打了出来

第一次运行 `CfirMacroTokenPositionTest` 的输出：

```
token 位置不得产出任何语义候选，实际 {
  cfir.keyword=[let, if, return],
  cfir.index=[Foo, ArgOpt, ...整个 SDK 的全部声明名...]
}
```

属性宏的 token 序列里，关键字区段给了关键字、索引区段给了**整个 SDK**。

### 根因：分类入口不统一

11.4 给 `CompletionRequest` 加了 `positionElement`（承载光标处的原始叶子），
但**只接进了 CDoc 区段**——其余 7 个区段与期望类型过滤器仍在分类 `completionPosition`。

那是**分析位置**：对于不在仓颉 PSI 里的位置（文档注释、宏 token），
它的最近 `CjElement` 祖先是**整个文件**，`classify` 只会得到 `EXPRESSION`。
于是所有区段都按表达式位置产出候选。

### 修法

加请求级入口 `CompletionRequest.positionContext`（内部走 `positionElement`），
把 8 处调用点统一过去，并在 KDoc 里写明「区段不要各自写
`classify(request.completionPosition)`——那条路会在这些位置上静默走错」。

### 用例为什么手写区段清单

`CfirMacroTokenPositionTest` 不从服务取区段，而是手写全部 8 个：
服务入口需要进度指示器（测试线程上为 null），而要验证的正是
「**所有**区段都不产出」——漏掉任何一个，结论就不成立。

用例同时断言区段在非仓颉 PSI 的叶子上**不抛异常**：一个没做位置守卫的区段
在那里抛异常，表现就是「在宏里补全整条管线崩掉」。

---

## 11.5② 降级：名称发现通道里混进了「遍历源码」（2026-10-08）

### 刻意失败的降级用例把缺陷打了出来

按 11.5② 写的降级用例（宿主**不注册**名称索引）期望「只能靠索引找到的源码模块候选缺席」，实际：

```
索引不可用时不得给出只能靠索引找到的源码模块候选，实际 [indexedLoad, ArgOpt, ...整个 SDK...]
```

`indexedLoad` 位于依赖模块的**源码**里。索引服务没有注册，它却出现了。

### 根因：`.cjo` 名字视图并不只覆盖 `.cjo`

`CaCfirSymbolProvider.getDeclarationsByNamePrefix` 的第二条通道写的是
`analysisSession.cfirSession.symbolProvider.symbolNamesProvider`，注释称它「已组合本模块与全部依赖，
内部走 `CjoExportedTopLevelNamesResolver`」。前半句对，后半句只对**库那一半**：

`LLModuleWithDependenciesSymbolProvider.symbolNamesProvider` 组合的，是
`providers`（本模块自身）与 `dependencyProvider.providers`（各依赖）**各自**的 `symbolNamesProvider`。其中——

| 符号源 | 名字从哪来 | 代价 |
|---|---|---|
| `AbstractCfirDeserializedSymbolProvider`（`BINARIES` 来源） | `CfirDeserializedSymbolNamesProvider` 读 `.cjo` 包头 | 便宜 |
| `LLCangJieStubBasedLibrarySymbolProvider`（`STUBS` 来源） | 库侧声明 provider | 便宜 |
| `LLCangJieSourceSymbolProvider` | **源码**声明 provider | 与项目规模成正比 |

第三类被一起枚举了。它的声明 provider 在 standalone/LSP 宿主下是
`CangJieStandaloneDeclarationProviderFactory` → `CangJieStandaloneSourceFileCollector.collect(scope)`
→ `moduleProvider.allSourceFiles` → `VfsUtilCore.visitChildrenRecursively` **逐个 `.cj` 文件建 PSI**。
（只读核对 IDE 插件：`CaIdeScopeCangJieFileCollector.collectSourceFiles` 同样遍历 `allSourceFiles`，
`collectCompiledFiles` 再遍历每个模块的 `.cjo`——同一个缺陷形态，实现搬到了插件侧。）

于是「按前缀发现名字」退化成「每键入一次遍历一遍项目源码」，这正是 `CaSymbolIndexQueryService`
存在的理由；而 LSP 宿主此前**没有注册索引服务**（见下），所以它唯一的源码侧名字来源就是这条兜底路径：
候选正确，代价与项目规模线性相关。

### 修法：把「非源码」做成符号源自己声明的属性

`CfirSymbolProvider` 新增 `libraryDeclarationsNamesProvider`（默认 `CfirEmptySymbolNamesProvider`）：

- `AbstractCfirDeserializedSymbolProvider` / `CfirDeserializedSymbolProvider`：`= symbolNamesProvider`（`.cjo`）；
- `LLCangJieStubBasedLibrarySymbolProvider`：`= symbolNamesProvider`（stub 库）；
- `CfirBuiltinSymbolProvider`：`= symbolNamesProvider`（合成 builtins，`Int64` 这类名字没有源码文件）；
- `CfirSwitchableExtensionDeclarationsSymbolProvider`、`LLDanglingFileDependenciesSymbolProvider`：透传委托侧
  （包装层只收窄行为，不改变「哪些名字属于库侧」）；
- `LLCombinedSymbolProvider` / `LLModuleWithDependenciesSymbolProvider` / `CfirCompositeSymbolProvider`：
  组合子 provider 的库侧视图（`by lazy` 是**必须**的——`providers` 由子类构造参数提供，基类初始化期还读不到）；
- `LLCangJieSourceSymbolProvider` 不覆写 → 贡献为空。

`symbolNamesProvider` 语义**未动**——解析路径（`mayHaveTopLevelClassifier` 等）仍看全量名称，
只有**发现**通道改走库侧视图，因此候选符号集合不因换来源而变。

### 验证时打出来的第二个缺陷：默认值取「未知」会把整个组合拖成 `null`

第一版把默认值写成 `CfirNullSymbolNamesProvider`（`getPackageNames()` 返回 `null`，语义是「**无法枚举**」），
结果**既有的** `CaSymbolProviderBinaryNamesTest` 转红——`Int64`、`Option` 全部找不到。
探针输出（临时 println + JUnit XML 的 system-out）：

```
[PROBE lib] self=[LLCangJieSourceSymbolProvider]
[PROBE lib] deps=[CfirCompositeSymbolProvider]
[PROBE lib] depCmp=[CfirDeserializedSymbolProvider, CfirBuiltinSymbolProvider]
[PROBE lib]   inner=CfirDeserializedSymbolProvider libPkgs=46 allPkgs=46
[PROBE lib]   inner=CfirBuiltinSymbolProvider     libPkgs=null allPkgs=2
[PROBE lib] result=CfirDelegatingCachedSymbolNamesProvider pkgs=null
```

真正的 `.cjo` provider 报出了 46 个包（`std`、`std.core`…），但组合的并集是
`flatMapToNullableSet`——**任一子视图返回 `null`，整组合就返回 `null`**，
于是 `collectLibraryDeclarationNames` 直接 early-return，46 个包一起消失。

两处都改了：

1. 默认值从「未知」改成**空视图**（`CfirEmptySymbolNamesProvider`）。对源码符号源来说，
   「它有哪些库侧名字」的答案是确定的——**一个都没有**，这是「知道没有」而不是「不知道」。
   这个区别在组合时是硬约束：组合方按并集合并，「不知道」会污染整组。
2. `CfirBuiltinSymbolProvider` 归入库侧。**取证**（临时去掉该覆写再跑一次既有用例）：
   `getDeclarationsByNamePrefix("Int64")` 只剩 `AtomicInt64`、`Int64Less` 这些**含子串**的名字，
   `Int64` 本身消失——即 `std.core` 的 `.cjo` 并不导出 primitive 名，
   它们只由这个合成 provider 暴露（它按定义暴露「基础包 + `std.core`」两个包的
   primitive 与 BuiltInDecl 名）。所以这一路是**必需**的，不是补齐语义的顺手一笔。

顺带把两处 `filterNot { it === CfirNullSymbolNamesProvider }` 删掉：默认值改空视图后它们成了死代码，
而且「靠组合方记得过滤」本身就是脆弱设计——现在**默认值就保证了不污染**，
漏覆写某符号源的表现退化为「该符号源缺席」，不会波及其他符号源。

修好后库侧视图 47 个包（全量视图 48 个，差的那一个正是源码模块自己的包）。

### LSP 侧：索引服务从来没被注册过

`AnalysisApiLspServiceRegistrar` 只加载 `cangjie-analysis-api-cfir.xml` 与 `cangjie-cj-references.xml`，
并按「手工补 standalone provider」的既有写法注册 declaration/package provider——
`CaSymbolIndexQueryService` 不在其中（它只在 `cangjie-analysis-api-standalone.xml` 里有条目）。
已按同一写法补上 `CaSymbolIndexQueryService → CaStandaloneSymbolIndexQueryService`。
说明：LSP 的补全目前仍走旧实现（不改本管线，接入是 §15 的工作），本项只是把服务补齐，
为 §13.1／§15 的前置；服务是惰性实例化的，未查询时无代价。

### 验证（三层，各锁一条界线）

新增用例：

- 分析层 `SourceDeclarationsNeedHostIndexTest`（**无**索引宿主）：源码依赖模块的 `sourceOnlyLoad`
  不得被发现；`Option`（库侧）必须照常发现——降级是「少给」而不是「不给」。
- 补全层 `CfirIndexDegradationTest`（**不注册**索引）：`indexedLoad` 不得成为候选，且候选集合非空。

执行记录：

| 套件 | 用例 | 失败 |
|---|---|---|
| `:analysis:analysis-api-standalone:test` | 713 | 0（含新增用例；基线 712/0） |
| `:analysis:low-level-api-cfir:test` | 136 | 4（`CfirCjmpCallerFirstDiagnosticsTest` **既有基线**，见 §7.4） |
| `:analysis:analysis-api-cfir:test` | 1623 | 0（与 §7.4 记录的基线一致） |
| `:cfir:providers:test` | 28 | 0 |
| `:cfir:cfir-serialization:test` | 115 | 0（5 skipped） |
| `:code-insight:completion:impl-cfir:test` | 64 | 0（含新增用例；此前 63/0） |

其中 `CfirIndexCompletionSectionTest`（注册索引）保持绿，说明源码模块候选确实由索引给出；
`CaSymbolProviderBinaryNamesTest`（无索引宿主的 `.cjo` 通道回归）在修好「默认值污染组合」后恢复绿。

### 交接：这条改动要求宿主自己提供索引

修好后**源码侧名字只能来自宿主索引**，因此宿主必须在改动生效前注册 `CaSymbolIndexQueryService`：

- LSP：本次已在 `AnalysisApiLspServiceRegistrar` 补上（standalone 实现，可直接用）。
- IDE 插件：**未做**（属 §6.6② 的范围，不在本工作区）。在 IDE 注册索引实现之前，
  「未导入的源码模块声明」这一类候选在 IDE 里会整类缺席——这是 13.1 要求的降级形态，
  不是可长期保留的状态。

---

## 15.1 §7.11.0 前置最小实验：三项结论（2026-10-08）

任务要求三项结论一并写入本文件，故本节即 15.1 的交付物。**结论：走分支 B（直接驱动管线）**。

### ① LSP 生产容器能否提供 `Editor` / `CompletionProcess` → **不能**

判定依据是**类路径与装配事实**，不是对某个容器的运行时探测。三条独立证据：

1. **`:lsp` 的类路径里没有编辑器**。`intellijCore()` = `project(":dependencies:intellij-core")`
   （`repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/intellijDependencies.kt:19`）。
   该 jar（`dependencies/intellij-core/build/libs/intellij-core-1.1.1.jar`）中
   `com/intellij/openapi/editor` 下的类数为 **0**——`Editor`、`EditorFactory` 都不在其中。
2. **平台 `core` 制品只带「文档/属性」子集**。`com.jetbrains.intellij.platform:core:253.29346.379`
   在 `com/intellij/openapi/editor` 下有 42 个类，但全是 `Document`、`RangeMarker`、`TextAttributes`、
   监听器等；**没有 `Editor.class`，也没有 `EditorFactory.class`**（编辑器服务与 UI 在
   `editor`/`platform-impl` 产品模块，`:lsp` 未依赖）。这与 `lsp/build.gradle.kts` 的依赖清单一致
   （`analysis`/`code-style`/`code-style-impl`/`indexing`/`refactoring`/`usage-view` + core）。
3. **无头容器也不注册 `EditorFactory`**。`CangJieCoreEnvironment.create`（生产与测试**同一入口**，
   只有 `mode` 取值不同）→ `CangjieCoreApplicationEnvironment` → `CangJieHeadlessPlatformBootstrap`，
   全程无 `EditorFactory` 注册；平台侧 `com/intellij/core/CoreApplicationEnvironment.class`
   （`core-impl` 制品）的常量池里也**没有 `EditorFactory` 任何引用**，只有
   `Document`/`DocumentImpl`/`FileDocumentManager`。仓内全量 grep `EditorFactory` 亦只有两处：
   测试夹具注释、以及 `CangJieHeadlessSuggestedRefactoringProvider` 的说明
   （后者正是「平台默认实现要监听 `EditorFactory`，无头宿主因此换实现」的同类先例）。

因此分支 A 不成立：**不注册 `completion.contributor`**，由注册器构造 `PlatformCompletionRequest`
直接驱动候选管线。补充说明：本项**未做运行时探测**（判据在依赖与装配层面已经封闭，
且当时共享 Gradle 缓存被其他会话占用导致无法执行测试）；若要再上一道保险，
可在 LSP 集成测试里断言 `EditorFactory` 不可解析——但它在编译期就不在类路径上，写不出这样的测试。

### ② 分析跑在哪份 `CjFile` 上 → **B-2，且这条路径已经存在并在用**

现状（`AnalysisApiLspSemanticSupport.analyzeSnapshot`）：
`AnalysisApiPsiDocumentFactory.createAnalyzableSnapshot(document)`
→ `upsertSnapshot(document)` 得到 PSI 文件，经 `LspAnalysisPsiFileFactory.createFile(project, uri, fileName, text)`
（即 `LspAnalysisVirtualFile` 那条路，**不直接引用**其 private 类）
→ `projectStructureState.useSiteModuleForOpenDocument(uri)` 取**真实 use-site 模块**
→ `analyze(snapshot.useSiteModule) { ... }`。

即 LSP 今天就是「直接分析 LSP 侧 `CjFile` + 真实模块」＝ 设计里的 **B-2**，并已被诊断/hover/旧补全长期使用，
**不是待建能力**。B-1（`analyzeCopy` + `CaDanglingFileModule`）因此不再需要，也不再受 D-3 阻塞。
代价照设计记：B-2 与 IDE 的补全副本语义**不等价**，故欠一条「两端分析语义等价性」对照用例
（由 §7.11.6 的新旧/两端对比测试承担，不得省略）。

#### ⚠️ 2026-10-08 晚：这条结论**不完整**，实现时被 dummy 修正打了出来

上面的判定来自**既有旧路径**的读码：它只需在**完整标识符**上取引用候选，因此不需要任何 PSI 改写。
但新路径（走 `CfirCompletionService`）必须先做 **dummy 修正**——把光标处的半截标识符
换成语法合法的占位表达式，否则会在裸 dummy 上分析并静默得到零候选（15.4 前置①存在的理由）。
`CompletionDummyIdentifierProvider.provideFor(file, offset)` 是一次 **PSI 写**
（`replaceWithPlaceholder` → `expression.replace(...)`），且要求在命令内执行。

于是「直接分析 LSP 那份 `CjFile`」与「先改写它再分析」互相冲突：改写会污染
`LspDocumentStore` 拥有的那份文档（之后所有诊断/hover 都会看到占位符）。

因此 ② 的准确表述应是：**候选分析可以走 B-2，但 dummy 修正必须在一份副本上做**，
即「自建副本 → 在副本上修正 → 分析副本」。这与设计的 B-1 只差**副本怎么造**
（B-1 用 `analyzeCopy` + `CaDanglingFileModule`；这里用 LSP 侧同一份文本造第二份 `CjFile`）
——而**副本的模块上下文**恰恰是设计点名要实验判定的那一格，尚未验证。
补记：`performNamedArgumentInsertion` 的实现里也没有「删掉平台写下的名字」这一步。

**下一步（取代原判定）**：写实验——在 LSP 宿主里造副本、在命令内做修正、把它送进
`CfirCompletionService.complete`，看候选是否产出、以及副本的 use-site 模块是否仍由
`projectStructureState.useSiteModuleForOpenDocument(uri)` 给出。结论落定前，本项**不得**按已判定处理；
若副本路线不成立，则回到 B-1 并接受 D-3 成为阻塞项（须按 §12.9 给出记录在案的决策）。

#### 实验结论（2026-10-08 晚，已跑通）：**「自建副本 + 修正副本」可行**

`LspCompletionPipelineProbeTest`（`:lsp:test`，1 用例）按生产容器跑通整条链路：造带 dummy 标记的副本
→ EDT 写命令内 `provideFor` 修正 → `analyze(module)` + 进度指示器 → `cfirCompletionServiceOrNull(project).complete(...)`
→ 拿到含 `buildUser` 的候选。因此：

- **副本的 use-site 模块**仍由 `projectStructureState.useSiteModuleForOpenDocument(uri)` 给出（先由一次语义请求触发
  `upsertSnapshot`），**不需要** `CaDanglingFileModule`，**不受 D-3 阻塞**；
- 副本用 `LspAnalysisPsiFileFactory.createFile(project, uri, fileName, text)` 造（同一 URI、新的 light virtual file），
  路径参与 scope 判定，因此能落进正确的模块作用域；
- 形态确定为 **B-2 的变体**：分析对象是副本，但模块上下文取自真实 use-site 模块。

实验过程打出**三条生产容器约束**（每一条都是失败一次才发现的，缺任一条都表现为**静默零候选**）：

1. **`CangJieCompletionServiceRegistry` 也必须注册**。`CfirCompletionServiceProvider.service(project)` 内部走
   `completionServiceRegistry(project)` → `project.service()`；未注册时抛异常，而
   `cfirCompletionServiceOrNull` 用 `runCatching` 把它吞成 `null`。已在 `AnalysisApiLspServiceRegistrar` 补上
   （与 `CfirCompletionServiceProvider` 同批，两条都是分支 B 的手工注册义务）。
2. **dummy 修正是 PSI 写，必须在 EDT + 写命令内做**。测试线程上直接调
   `CommandProcessor.getInstance().executeCommand(project, ...)` 会**静默不执行**
   （实测：命令块里捕获的变量出来仍是 `null`，不抛异常）；照仓库既有夹具的写法
   `EventQueue.invokeAndWait { WriteCommandAction.runWriteCommandAction(...) }` 才生效。
   服务端侧同理：RPC 线程不是 EDT，实现必须自行 marshal。
3. **`CangJieCompletionService.complete` 需要进度指示器**。RPC/测试线程上直接调会抛
   `getProgressIndicator(...) must not be null`；必须自开 `ProgressManager.runProcess(..., EmptyProgressIndicator())`。

### ③ `invocationCount` 与 `completionKind` 的映射规则

IDE 侧的来源是 `PlatformCompletionRequest`：`kind = completionKindOf(parameters.completionType)`、
`invocationCount = invocationCountOf(position)`（读 PSI 上的 `INVOCATION_COUNT_KEY` 用户数据）。
LSP 侧两者都必须自行定义，规则如下：

- **`kind` 恒为 `CompletionKind.BASIC`**。LSP 协议没有「智能补全」触发（`triggerKind` 只有
  Invoked / TriggerCharacter / TriggerForIncompleteCompletions），映射不出 SMART；
  因此 `CfirExpectedTypeCompletionFilter` 在 LSP 路径上不生效——这是**有意的**，SMART 仍是 IDE 专属触发，
  不得凭客户端能力瞎猜一个 SMART 出来。
- **`invocationCount` 来自 LSP 会话存储的重入计数**：`context.triggerKind == TriggerForIncompleteCompletions`
  （客户端因上一轮 `isIncomplete` 再次请求）语义上等价于「同一位置的重复调用」，
  计数取 `1 + 同一 (uri, documentVersion, offset) 的重复请求数`；`Invoked`/`TriggerCharacter`/无 `context` 时为 `1`。
  这正对应索引区段里 `request.invocationCount > 1` 那条「放宽前缀匹配」的分支
  （见 `CfirIndexCompletionSection`），映射成立的前提是 15.7 的会话存储先落地。
  注意：不能拿「前缀变长了」当重复调用——那是用户继续输入，由 `prefix` 本身表达。

### 对后续任务的影响

- **15.2 / 15.3 是分支 A 的任务**（prime `completion.contributor`、上游白名单增补以装载补全描述符）。
  按分支 B 执行时这两项**不适用**，须在 tasks.md 里按「分支 B 不适用 + 理由」记录，不得静默跳过。
- **15.4 的四项前置全部成为必经项**（无参占位修正入口、`documentStamp` 接 LSP 文档版本、
  在 LSP 容器注册 `CfirCompletionServiceProvider`、转换对象改读 `CompletionSnapshot`）。
- 15.1 已记录 ⇒ 按任务措辞，第 15 组自此视为**已开始**。

---

## 15.x 分支 B 起步与待验证项（2026-10-08）

按 15.1 的分支 B 结论开工，已完成**接线**部分：

- `lsp/build.gradle.kts`：新增 `:code-insight:completion:{contracts,impl-shared,impl-cfir}` 三模块依赖
  （`implementation` + `testImplementation`）。分支 B 仍需要它们——直接调用 `CangJieCompletionService`
  的公开契约与服务入口；不需要的是「prime `completion.contributor`」那一半。
- `AnalysisApiLspServiceRegistrar`：注册 `CfirCompletionServiceProvider`（15.4 前置③）。
  上游补全描述符只有 `completion.contributor`、没有 `<projectService>`，而 `cfirCompletionServiceOrNull`
  走 `project.getService(...)`；不注册就是取到 `null` → 零候选且不报错。

**已核实为「已存在」，无需再做**：15.4 前置①的「无参占位修正入口」在 8.4 就落地了——
`CompletionDummyIdentifierProvider.provideFor(file, offset)`（提交 `c4c6c55af`，含两个回归用例），
其 KDoc 明写「无头宿主（LSP）同样拿不到平台补全参数」，与分支 B 的需求一致。

### 待验证 → 已补跑（2026-10-08 晚）

阻塞原因：另一会话的 Gradle 守护进程（由 IDE 以 debug 模式启动、带 `-agentlib:jdwp`）长时间持有共享的
`~/.gradle/caches/9.4.0/generated-gradle-jars` 锁，十次启动全部超时失败；`jcmd` 确认该进程当时空闲、
不是死锁，故先未干预，经用户确认后结束该进程（PID 7572），随后一次性补跑：

| 待跑项 | 结果 |
|---|---|
| `:lsp:compileKotlin` | **BUILD SUCCESSFUL**（三模块依赖与两处服务注册可编译） |
| `:lsp:test --tests '*CangjieSemanticFeatureIntegrationTest*'` | **2 用例 / 0 失败**（LSP 容器在新增两项注册下正常启动并解析语义能力） |
| `:code-insight:completion:impl-shared:test` | **66 用例 / 0 失败**（与既有基线一致） |

至此 15.x 的接线部分有了编译与运行证据；15.5/15.7 的实现仍待做。

---

## 15.5 LSP 候选字段映射与插入编辑（2026-10-08）

分支 B 的第一段实现，两处都在 `:lsp` 新增的 `org.cangnova.cangjie.lsp.completion` 包内。

### 字段映射

- `LspCompletionItemKindProvider`：把快照映射成 LSP 类别，**不再一律 `Text`**（旧实现全 `Text`）。
  判定只用快照里确实冻结的事实：`kind`（形状）、`identity.kind`（类型/符号/关键字/包/文档）、
  `tailText` 是否以 `(` 开头（可调用形态）。快照回答不了的细分（`class`/`interface`/`struct`、
  成员是字段还是属性）取 LSP 中性项并在 KDoc 里注明——**不按名字或文本猜**。
- `LspCompletionSortingUtil`：`sortText` = 列表下标零填充 6 位。刻意**不用权重值**：
  权重是分析期的位段编码，把它暴露出去等于让客户端解释内部编码，位段一调就与 IDE 端漂移。
- `LspCompletionItemConverter`：`label` = `presentableText`、`labelDetails.detail` = `tailText`、
  `labelDetails.description` = `typeText`（两者都空则**不设该字段**）、空 `textEdit` + `apply` 命令 +
  `data` 会话键。`textEdit` 是 `Either<TextEdit, InsertReplaceEdit>`，取左支（`Either.forLeft`）——
  这一处类型与「`Either<String, TextEdit>`」的直觉不同，是实测 API 后改的。

### 插入编辑

`LspCompletionEditCalculator.candidateEdits(text, replaceStart, replaceEnd, snapshot)`：

- **括号/caret 策略直接复用共享实现** `performCangjieInsertion`（不重写「已有括号不补一对」等判断）；
- 客户端拿到的是**空** `textEdit`，所以「替换用户已键入的前缀」必须由服务端算出来（一条删除编辑）；
- 插入结果从「变更中坐标」折算回**原始文本**坐标：`原始 = 当前 − 之前插入长度和 + 被删前缀长度`。
  这一格**踩过一次**：只加上前缀长度会让「括号」编辑落到文本之外（实测报
  `Range [19, 28) out of bounds`），因此加了「插入偏移单调不减」的 require 把前提钉住。
- 已知限制（显式记录）：`applyEdit` 不携带 caret，客户端应用编辑后默认把 caret 放在编辑末尾，
  因此「插入 `foo()` 后 caret 停在括号内」只能由客户端实现（§7.11.8）；服务端**不**为此改走 snippet——
  那会让插入文本随客户端能力而变。

用例 `LspCompletionItemConverterTest`（7）+ `LspCompletionEditCalculatorTest`（6），断言方式是
**把编辑应用到原文再看结果文本**：只断言编辑条数或区间会让「删除与插入错开一格」照样通过。

### 顺带查到一个宿主侧可疑缺陷（未修，记录在案）

命名实参候选的 `insertion.namedArgumentPrefix` 是**整个** `"beta: "`（设计 §7.7「调用插入 `name:`」）。
LSP 路径按此把它**整体**插入，用例 `namedArgumentCandidateInsertsWholeNamePrefix` 锁定结果文本为 `beta: `。
但 IDE 路径是「平台按 `lookupString` 插入 `beta`」+「处理器再插入 `"beta: "`」——
按现有代码组合会写出 `betabeta: `（`LookupElementFactory.create` 用 `lookupString`，
`performNamedArgumentInsertion` 只写前缀、不删平台写下的名字）。
既有用例只覆盖了 `performNamedArgumentInsertion` **单独**作用于文本接缝的情形，
没有覆盖「平台插入 + 处理器追加」这一步组合，因此这个坑至今没被任何测试碰到。
**未修的理由**：该路径属宿主侧 typed-handler 插入接线（10.2/8.4 未勾选，且 `PsiImportPlanApplier`
在生产代码里**没有调用点**），本轮无法端到端验证；修法也取决于接线方式（处理器改为替换，
或 `namedArgumentPrefix` 改回 `": "`）。**确认方式**：宿主插入链路接通后，实测一次命名实参补全。

---

## 15.6/15.7 命令路由、能力声明与三条协议事实（2026-10-08）

实现 `LspCompletionCommandHandler`（`workspace/executeCommand` 的 `applyCompletion` 分支）+
`CangjieWorkspaceService` 路由 + 会话存储接入 `CangjieServerContext`，
并让 `CangjieLanguageServerDescriptor.executeCommands` 默认声明该命令。

服务端的职责边界照设计写死：**只做两件事**——算编辑、把编辑经 `workspace/applyEdit` 发给客户端。
文本由客户端落盘，因此服务端不写文档、不涉 EDT 写事务、不做撤销分组。
每条失败路径都返回**给用户看的**明确错误：会话已过期／候选下标越界／文档已关闭／
文档版本已变／客户端不支持 applyEdit／编辑计算失败（区间越界、导入冲突）。

### 实现中被实测打出的三条协议事实

1. **命令实参到达时是 `JsonPrimitive`，不是 `String`/`Number`**。第一版直接强转，服务端日志是
   `applyCompletion 缺少会话键：实参 [JsonPrimitive, JsonPrimitive]`。现在同时接受
   `JsonPrimitive`（按 `isString`/`isNumber` 判）与进程内直接构造的 `String`/`Number`。
2. **LSP 的服务端能力里没有 `applyEdit` 字段**。设计 §7.11.3 写「在能力工厂声明
   `capabilities.workspace.applyEdit = true`」——这一条**在协议上不成立**：
   核对 lsp4j 1.0.0 源码，`WorkspaceServerCapabilities` 只有
   `workspaceFolders`/`fileOperations`/`textDocumentContent`。该能力只在**客户端**侧声明，
   服务端的正确做法是**据此决定发不发请求**：客户端没声明时直接报错，
   而不是发一个没人处理的请求（表现同为「接受候选后没有反应」）。
   协商器因此新增 `workspaceApplyEdit`，命令处理器按它 gate。
3. **普通异常经 LSP4J 只会在客户端侧得到一句 `Internal error.`**：服务端日志有完整原因
   （`fallbackResponseError: Internal error: java.lang.IllegalStateException: …`），客户端拿不到。
   失败必须经 `ResponseErrorException(ResponseError(code, message, null))` 抛出，
   消息才会到用户面前。这也解释了为什么失败消息要写成「给用户看的」那一句。

另外一处 API 事实：lsp4j 1.0.0 的客户端方法名是 **`applyEdit`**（`@JsonRequest("workspace/applyEdit")`），
不是 `workspaceApplyEdit`——照直觉写会编译不过（这次正是这么发现的）。

### 验证

`LspCompletionCommandHandlerTest`（6 用例，走真实 JSON-RPC 双端）：

- 接受候选 → 服务端恰好发出一条 `workspace/applyEdit`，其编辑应用到原文得到 `buildUser()`；
- 会话过期 / 下标越界 / 版本已变 / 客户端不支持 applyEdit → 各自报**对应**的明确错误，
  且**不发** `applyEdit`；
- 能力声明：`executeCommandProvider.commands` 含该命令（**不**断言服务端 applyEdit，理由见上）。

回归（一次一个类，按 §10 的 LSP 纪律）：能力协商 3/0、协议契约 3/0、启动集成 3/0、
生命周期 3/0、语言服务器 4/0、语义特性 2/0、字段映射 7/0、编辑计算 6/0、
会话存储 4/0、导入编辑 4/0、容器探针 1/0。


---

## 15.8 门面切到共享管线，旧实现降为 `legacyCompletion`（2026-10-08）

`AnalysisApiCangjieAnalysisFacade.completion` 默认走**与 IDE 共用的候选管线**，形态即 15.1② 的实验结论：

1. 造带 dummy 标记的**副本**（标记常量由 `CompletionDummyIdentifierProvider.DUMMY_MARKER` 公开复用，
   避免「标记拼错 → 占位替换找不到位置 → 静默零候选」）；
2. 在 **EDT 的写命令**内做修正（RPC 线程直调 `CommandProcessor` 会静默不执行）；
3. 管线调用包一层**进度指示器**（RPC 线程没有，直接调会抛 `getProgressIndicator(...) must not be null`）；
4. 候选存进会话存储，客户端稍后接受时由 `applyCompletion` 按会话键取回；
5. 转成带 `applyCompletion` 命令与会话键的 `CompletionItem`（`kind` 恒 BASIC、
   `invocationCount` 由会话存储按「同位置重复请求」计数）。

实测确认：**不需要** `analyze(module)` 包装——各 section 自己 `analyze(file)`，副本按路径落进正确模块作用域
（端到端用例首次运行即通过）。

旧实现（引用 variants + 文件作用域 + 全工作区扫描）原样保留为 `legacyCompletion`，
由 `descriptor.useLegacyCompletion`（默认 false）打开：两条路径不混用——混用会让候选质量类验收无法判定。

会话失效有**两道**防线：`didChange`/`didClose` 在源头丢弃该文档的会话；命令侧的版本复验是兜底
（失效回调没走到时仍拒绝写入）。两条各有一个用例（写文件变更用例时先踩到「先命中已过期」，
因此把两条路径拆成两个用例分别锁定）。

用例：`LspCompletionPipelineIntegrationTest` 2/0（断言「走的是哪条路径」不看候选文本，
而看只有新路径才有的字段：命令 + 会话键 + sortText；并确认旧特征「类别一律 `Text`」不出现）。
回归一次一个类：语义特性 2/0、协议契约 3/0、启动集成 3/0、生命周期 3/0、语言服务器 4/0、
容器探针 1/0、命令处理器 7/0。


## 15.9 新旧候选集对比（2026-10-08）

`LspCompletionNewVsLegacyTest` 在**三个固定位置**（函数体起始 / let 初始化 / 二元表达式右侧）各跑两次——
一次默认路径、一次打开 `useLegacyCompletion`——断言**新集合 ⊇ 旧集合**，且默认路径的候选**全部带管线标记**
（命令 + 会话键），即旧实现的全工作区遍历不在默认路径里生效。

对比位置取**空前缀**（`let u = ` 之后、还没敲字符），理由是两边回答的问题才相同：
新路径**按已键入前缀过滤**（那半截标识符就该过滤掉不匹配的名字），
旧实现完全不看前缀、把文件作用域与工作区名字直接倾倒。
写这个用例时先踩了一次：拿「敲了 `buil` 的位置」比，得到
`旧有而新缺：[consume]`——那不是缺陷，是新实现**正确地**没有给不匹配前缀的名字。
同一轮还踩到测试自身的坑：`indexOf("buil")` 命中了夹具里 `buildUser` 的声明名，
补全位置落到了函数名上（那里本就该没有候选），改成锚定 `= buil` 再定位。


## 15.10 LSP 侧测试套件（部分，2026-10-08）

已覆盖：无候选返回合法空列表（原始字符串内部）、`isIncomplete` 与实际是否穷尽一致
（返回完整列表）、import 编辑正确性、会话失效的两条路径、`applyEdit` 能力声明与命令路由。
新增 `LspCompletionProtocolBoundaryTest`（2 用例）。

**`$/cancelRequest` 未做，且不是接线问题**：`CangjieRequestExecutor` 是单线程队列，
LSP4J 对 `$/cancelRequest` 的取消落在它返回的那个 future 上，而我们的工作跑在自己的线程里——
取消不会中断工作线程，`ProgressManager.checkCanceled()` 也看不到任何标志。
要真正支持需三步（下一步的设计）：
1. 补全调用时用一个**可取消的进度指示器**（`isCanceled` 读原子标志），让管线里的
   `checkCanceled()` 真的能抛出；
2. 把该标志绑到请求 future 的取消上，并**不依赖线程中断**——共享单线程执行器上
   残留的中断状态会污染下一个请求；
3. 把 `ProcessCanceledException` 映射成 JSON-RPC 的 `RequestCancelled`（-32800），
   而不是 `Internal error`。

`resolve` 填 documentation 同样未做：快照契约里没有 documentation 字段，
要先决定「快照携带渲染后的 CDoc」还是「resolve 时按身份重进分析」。


## 15.10 / 13.1 `$/cancelRequest`：取消到达管线（2026-10-08）

取消不会自动到达工作线程：客户端 `$/cancelRequest` 后 LSP4J 对挂起的请求 future 调
`cancel(true)`（`RemoteEndpoint` 收到取消通知即如此），而语义工作跑在 `CangjieRequestExecutor`
自己的线程上——`supplyAsync` 的取消**不中断**正在跑的 supplier。做法：

1. `LspRequestCancellation`（可取消请求状态）随**请求上下文**传进管线；
2. 文档服务把 future 的取消（`CancellationException`）绑成取消状态翻转；
3. 门面在管线**前后**各查一次闸门，取消则抛 `RequestCancelled(-32800)`；
4. 进度指示器也接上取消状态（容器将来支持时会中途收场）。

### 实测打出的容器事实（**影响 13.1 的 LSP 分流**）

**本容器里 `ProgressManager.checkCanceled()` 不读推入的指示器**：`runProcess(..., 已取消的指示器)`
里调 `checkCanceled()` **不抛**（实测用例 `containerDoesNotYetHonourPushedIndicatorCancellation`
锁住现状），因为容器的 `ProgressIndicatorProvider` 未接线、全局指示器恒为 `null`。
后果：**靠进度指示器在管线中途收场这条路当前不通**——取消只能由闸门承担（管线仍会跑完，
但结果被丢弃）。这条与「管线里到处都有 `checkCanceled()`」的印象相反，属于必须知道的事实：
在 LSP 容器里那些检查是**空转**的。

还有一条 API 事实：`ResponseError.getCode()` 是 `int`（构造收枚举），拿枚举去比永远不相等
（写断言时踩到）。

用例 4 条，全部**确定性**（不靠 sleep 抢窗口）：已取消 → `RequestCancelled`；
请求期间到达的取消 → 结果被丢弃（闸门读两次，「第一次没取消、第二次已取消」的 token
精确模拟中途到达）；future 取消 → 状态翻转；容器现状。


## 13.2 补全统计接缝（前半，2026-10-08）

补全侧此前**没有任何统计层**（连候选量都看不到）。新增与仓颉其它性能信号同源的接缝
（同一个 `OpenTelemetry` 实例、同一条导出链路，不另起后端）：

- `CompletionStatisticsSink`（契约）：阶段耗时、候选量（**产出/交付分开记**）、未交付原因；
  三条硬约束写进 KDoc——只记数值（不记源码/文档/候选文本）、不阻塞主路径、宿主可选择不装；
- `OTelCompletionStatisticsSink`：指标名与属性名是**外部契约**（看板按名查询），集中一处；
- `CangJieCompletionStatistics`（项目服务）+ `installOtelCompletionStatistics(project)`：
  有 OTel provider 时装上，否则 noop；
- 接线：CFIR 服务记 `CANDIDATES` 与候选量、索引区段记 `INDEX_QUERY`、LSP 门面记
  `SETUP`/`RENDER` 与 `CANCELLED`/`POSITION_NOT_ALLOWED`/`SERVICE_UNAVAILABLE`、
  `applyCompletion` 记 `INSERT` 与 `SESSION_EXPIRED`/`VERSION_MISMATCH`。

**踩到主次颠倒的一格**：`completionStatistics()` 最初走 `project.service(...)`，
轻量夹具没注册该服务 → 直接抛异常，**8 个补全用例一起红**。
统计是旁路信号，缺了它最多是没数据，绝不该把被测功能弄挂：改为
`serviceOrNull(...) ?: NoopCompletionStatisticsSink`（这条也写进了函数 KDoc）。

另按实测修正两处 OTel API（1.39）用法：`counterBuilder` 已是 long（`ofLongs()` 只在
`DoubleCounterBuilder` 上）；`Attributes.of(String, ...)` 已移除，改用带类型的 `AttributeKey`。

用例 `LspCompletionStatisticsTest` 2/0：用记录式 sink 断言「该记的都记了」——
一次成功补全要看到候选量 + `CANDIDATES`/`RENDER`/`SETUP`，一次过期会话要看到 `SESSION_EXPIRED`。
**未做**：冷/热与大/小项目**可复测基线与预算**（需真实工程与具名测试配置文件）、
`resolve` 阶段耗时（`resolve` 本身未实现）。


## 15.5/15.10 `completionItem/resolve`：延迟文档（2026-10-08）

文档延迟到用户真要时才算（快照契约不持符号/会话；顺手算好会让每个候选都背一份渲染结果）。
分层：`CompletionDeclarationResolver`（impl-shared 窄接口 + 装配点）在 **impl-cfir** 实现
——身份键算法在那一层，恢复端必须同源；渲染复用门面既有 CDoc 渲染（与 hover 同一份）；
`data` 扩成 `{sessionKey, index}`（resolve 只拿到 item，按下标才回得到候选）。

### 两条硬事实

1. **符号不得跨会话带出**。第一版契约让 resolver 返回 `CaDeclarationSymbol`，
   会话外读它的任何成员都抛 `CaInaccessibleLifetimeOwnerAccessException: Called outside an
   analyze context`。改成「在调用方分析上下文内恢复并**立即使用**」（回调 `use`）；
   会话走显式参数（接口成员带接收者要求两个接收者同时可解析，调用点得写成 `with(resolver){...}`）。
   另：`fun interface` 不允许带类型参数的抽象方法 → 这里是普通 interface。
2. **本容器里 CDoc 取不到（记录当前行为）**：LSP 分析文件是 light virtual file，
   `CjDeclaration.docComment` 为空——文档注释附着走 stub/物理文件那条路。
   对照：仓内 `CfirIdeNormalAnalysisSourceModuleCDocProviderTestGenerated` 在真实文件夹具上 **2/0**。
   故 resolve 的「恢复」半可用、`documentation` 半在本容器恒空，已用
   `resolveCannotFillDocumentationInThisContainer` 锁住现状（容器支持后会失败并提醒改写），
   同时 `resolverRecoversTheSameDeclarationByCandidateIdentity` 单独锁住恢复这半。

用例 4/0；回归 impl-shared 66/0、impl-cfir 64/0、LSP 九类各自绿。


## 2.4 / 3.7 测试基建两处（2026-10-08）

### 2.4 dangling 夹具消费解析模式指令

夹具声明过 `IGNORE_SELF_MODE` 却**从不读它**——「副本走哪个解析模式」既选不了也验不了。
本轮：副本准备抽成 `DanglingFileFixturePreparation`（抽出来才能**直接验证**，否则只有走完整诊断管线才碰得到）；
抽象类按指令选模式；新增 `PREFER_SELF_MODE` 作为仓颉侧显式入口，同时声明两者直接报错。
两种模式的唯一差别是是否把平台 `PsiFile.originalFile` 指向原件——**探针实测**：该属性期望
`@NotNull PsiFile`（即平台自身的 dangling 钩子，不是仓颉侧属性；静态 grep 找不到声明是正常的）。

`DanglingFileFixturePreparationTest` 1/0 逐个模式验：原件文本不改、副本是原件复制、
副本保留原件顶层声明、两种模式各自的 `originalFile` 形状。用例**不跑分析**，故不受
PREFER_SELF 解析缺陷影响；后者与 6.8 关联，已在 tasks 里写明。
回归：`:analysis:analysis-api-cfir:test` 1625/0（其中 dangling 生成用例 70/0——默认仍是 IGNORE_SELF，行为不变）。

### 3.7 类型片段：类与文件类型都在，只缺工厂入口

`getExtraScopes` 的实现早已落地（任务描述里的「现为 `emptyList()` 桩」已过期）。
补的是**验收**：`CjTypeCodeFragment` 与 `CjTypeCodeFragmentType` 都在，
**只缺 `CjPsiFactory` 的工厂入口**，于是「类型片段」这条路在测试里根本走不到。
补上 `createTypeCodeFragment` 后 `CodeFragmentScopeExecutionTest` 4/0（块正例、表达式反例、
**类型反例+正例**、投影同源）。另记：`CjCodeFragment.getContentElement()` 是 Kotlin 声明的函数，
不合成属性，调用必须写括号。


## 12.1 / 12.2（分区内部分）制品与打包边界核对（2026-10-08）

12.1 的三项要求逐条**实测**：
- fat jar 模块显式列出三个合并输入（`contracts`/`impl-shared`/`impl-cfir`）并复用 `publishCangjieJarsForIde`；
- module 形态用 `api(project(...))` 依赖三者；
- 构建后 fat jar **147 个类全在 `org/cangnova/cangjie/ide/completion` 下**——也就是
  「不重复打包已有 common/psi/analysis 类」（12.2 的那一半）按内容成立；
  module 形态 jar 261 字节，与其它 module 制品**同形**（对比 analysis-api 等 7 个制品）。

12.2 还剩宿主侧接缝：`intellij-ide` / `deveco` 的 Version Catalog 与 substitution 不在本工作区。


## 5.6 适用性正反测试：补齐两条（2026-10-08）

六项里补全层原有三项（receiver 不匹配、权限/private 成员、同名导入-局部 shadow），本轮补两项，
且**每项都带正反对照**：约束失败的 extend 成员（`Box<Plain>` 上不出现、`Box<Shaped>` 上出现）
与 private extend 成员（不出现，同 extend 的公开成员出现）。只测「满足」那半不够——
判据整段失效时那半照样绿。

夹具语法先用 **cjc 1.1.3 实测**：泛型 extend 的合法形式是 `extend<T> Box<T> where T <: Shaped`；
直觉写法 `extend Box<T> where ...` 会被拒（`unexpected 'where' in non-generic declaration`）。
`private func` 在 `extend` 里合法，且从外部调用会报 `no matching function declaration` ✓。

### 顺带查出：`QualifyThenShorten` 没有任何生产者

`CompletionImportPlan.QualifyThenShorten` 全仓只有三处：契约定义、应用器的消费分支、
以及一条**手喂**它的用例——**分析层从不产出这个计划**。也就是说「写限定名 → 交给引用缩短」
这条路径当前是休眠的（应用器会做，但没人请求）。这与 5.6 的「缩短回归」一项直接相关，
也解释了 LSP 侧 import 编辑遇到冲突时报明确错误是当前唯一正确行为。

**未做**：5.6 的「旧三态调用者」回归属 A 侧；「缩短回归」在有人产出该计划之前无从回归。


## 5.5 候选可达性分类不再压平（2026-10-08）

设计 §6.6 的红线：**失败、未知与不可见不得全部压成 HIDDEN**。原实现正相反——
`CaCompletionCandidateStatus` 只有 `DIRECT/REQUIRES_IMPORT/HIDDEN`，生产端把三种情况都落进 `HIDDEN`：
`!visible`（权限/遮蔽）、`CaFileSymbol`（不是一类东西）、以及「既非直接可达也拿不到导入路径」（**判定不出来**）。
消费者因此分不出「该移除」与「不知道」，而未知被当成否定会让用户少候选且毫无提示。

改法：拆成四类（`HIDDEN`/`NOT_APPLICABLE`/`UNKNOWN` + `DIRECT`/`REQUIRES_IMPORT`），
并把行为规则写成**两个属性**——`shouldHide`（只有确定不可用才算）与 `isDeterminedUsable`
（只含能落地的两态；需要写确定性结果的动作按它判）。生产端按四类给出；
引用缩短的消费规则改按 `isDeterminedUsable`，**行为保持不变**（此前未知与不适用都由 HIDDEN 挡掉）。

用例 4/0，含**穷尽性互斥**：新增状态时忘了想清归类就会红——否则漏想的后果是它在某条路径上
被静默当成「可以」或「不可以」。行为未变的证据：既有 `AnalysisApiCfirComponentExecutionTest` 保持 5/0。
回归：analysis-api-cfir 1629/0、impl-cfir 66/0、impl-shared 66/0。

**5.5 剩下的一半已具备**：目标身份/组织名与 alias/冲突复验信息都在
（`CompletionImportTarget.importPath` 携组织名与 alias、`ImportConflict` 分析期给出、应用器写事务内复验）；
实际写入属第 10 组。**未做**：A 侧「任意位置局部遮蔽」的三态替代（设计 §6.6 现状条目），属更大的 A4/A5 面。

## 缺陷 D-3 修复：根因是漏移植「等价调用去重」，不是缓存与 require（2026-10-08）

任务 6.8。**已修**，工作树内验证通过。前两轮取证（2026-10-05）把根因记在
`CfirCacheWithInvalidation.getNotNullValueForNotNullContext` 与
`require(context == null || context.isPhysical)` 上——那两条都是**另外**的路径，
不产生「PREFER_SELF 返回 null」这个症状；本节给出实测根因与修法，上文取证结论保留不动。

### 实测根因

临时探针（工作树内 `D3ProbeTest`，验证后已删除）在 PREFER_SELF 副本里打印：

```
D3PROBE| symbolProvider=LLModuleWithDependenciesSymbolProvider
D3PROBE| filesByPackage=[FILE: d3Probe.cj]                       ← 包作用域只含副本
D3PROBE| topLevelFunctions=[CfirNamedFunctionSymbol(tracked), CfirNamedFunctionSymbol(tracked)]
D3PROBE| func[0] psi=FUNC: tracked file=COPY
D3PROBE| func[1] psi=FUNC: tracked file=MAIN
D3PROBE| resolveToSymbols.size=2 kinds=[CaCfirNamedFunctionSymbol, CaCfirNamedFunctionSymbol]
D3PROBE| resolveToSymbol=null
```

- 副本会话的合成 provider = 自身源码 provider（副本）+ 依赖 provider（上下文模块会话，含原件），
  **同一份声明以两种身份**同时进入候选集；
- 调用解析因此得到两个等价候选 → 判重载歧义 → `resolveToSymbol()`（内部 `singleOrNull()`）返回 `null`。
  这就是「PREFER_SELF 非局部解析返回 null」的真身。

### 上游对照：Kotlin 有这一步，仓颉整个类没移植

- Kotlin `ConeCallConflictResolverFactory.create` 的消解器链第一位是 `ConeEquivalentCallConflictResolver`；
  仓颉同一行**被注释掉**，且该类**全仓不存在**（只有那行注释提到它）。
- 判据里的关键一条是 `moduleData == 且 !areRedeclarationsEquivalent → 不等价`；
  仓颉侧 `CfirModuleData.areRedeclarationsEquivalent`（`cfir/cfir-common/.../CfirModuleData.kt:103`）
  与 `LLCfirModuleData.areRedeclarationsEquivalent`（`analysis/low-level-api-cfir/.../LLCfirModuleData.kt:116`）
  **都已移植却无人消费**——半移植机制的典型形态，也是「谁漏了」的判据。
- 上游行为有测试数据直接锁定：`external/kotlin/analysis/analysis-api/testData/danglingFileReferenceResolve/preferSelf/topLevelFunction.kt`
  期望同文件顶层 `call()` 解析到 **fake.kt（副本）**，`ignoreSelf/` 同文件期望解析到原件。

### 修法

- 新增 `cfir/resolve/src/.../calls/overloads/ConeEquivalentCallConflictResolver.kt`，逐条对齐 Kotlin：
  只处理非成员 callable、callableId 相同、**同一源码模块内的重声明永远不等价**（保住重定义/冲突重载诊断）、
  函数与属性不可互等、实参映射顺序一致、签名按 alpha-equivalence 比较；源码优先保序去重。
- `ConeCallConflictResolverFactory.create` 恢复 Kotlin 的顺序（置于链首）。
- **两处记录在案的偏差**：①签名比较改用 providers 层既有 `overrideSignatureKey`
  （Kotlin 用 `FirStandardOverrideChecker`；两者都不比较返回类型）；②仓颉侧签名键比较不需要 session，
  故消解器无状态、写成 `object`。
- **未启用**同处一并被注释掉的 `ConeIntegerOperatorConflictResolver`（整数运算符消解，与 D-3 无关，另案）。

### 验证

- 探针复跑：`resolveToSymbols.size=1`、`resolveToSymbol` 非 null 且落在 **COPY**
  —— 即验收要求的「PREFER_SELF 解析返回非 null」且语义为「解析到副本」。
- `AnalysisApiDanglingFileExecutionTest` 四处「已知缺口」断言翻转为正确行为
  （threadIsolation 的串行/并行、copyAndModeIsolation、uncommittedDocument），
  modification 用 pointers 那条只同步注释（其会话失效缺口是另一件事，未动）。
- 回归：`:analysis:analysis-api-cfir:test` **1629 用例 0 失败**（与 5.5 节同一基线）。
- `:cfir:resolve:test` 137 用例 1 失败 = `CfirMapArgumentsTest > Defaults > number of default arguments is counted`：
  **既有失败，与本次改动无关**——把本消解器重新注释掉后该用例仍失败，且它只跑 `CfirMapArguments`
  单阶段、不经过冲突消解器。
- `:cfir:analysis-tests:test` 8964 用例 44 失败：**全部既有，且本改动顺带修掉 5 个**。
  对四个失败类做同过滤器 A/B：**停用**本消解器 = 40 个用例名失败（1242 用例 48 失败），
  **启用** = 35 个（1242 用例 44 失败）——启用后少 5 个、一个都没新增，少掉的是
  `testCjmpBothImplicitReturnTypesMatch`、`testCjmpNamedParamName`、`testCjmpReturnTypeMismatch`、
  `testCjmpSpecificAbstractMemberNoExplicitModifier`、`testCjmpSpecificShadowingBeforeImplicitTypes`。
  剩余 35 个（宏语料 + cjmp 诊断语料）在停用态同样失败。
- `:analysis:low-level-api-cfir:test` 136 用例 2 失败：`CfirCjmpCallerFirstDiagnosticsTest` 同类 A/B——
  停用态 4/4 失败，启用后 2/4（**修掉 2 个**，剩余 2 个 `defaultArgument*CallerFirst` 为既有）。
- `:lsp:test` 84 用例 0 失败。
- **净效果**：本改动修复 7 个既有失败（5 个 cjmp E2E + 2 个 LL caller-first），未引入任何新失败。
  剩余 caller-first 与宏语料失败与本机制同族（跨模块同名声明），可作为 cjmp / 宏侧后续线索，不在本任务范围。

### 仍未做（如实记录）

`AbstractDanglingFileCollectDiagnosticsTest` 的**默认模式仍是 IGNORE_SELF**（Kotlin 夹具无指令时默认 PREFER_SELF），
`originalFile` 只在 IGNORE_SELF 下设置（这一半已与 Kotlin 一致）。把默认改成 PREFER_SELF 会踏入**另一条**路径：
非物理副本的 class-like 声明进入 `LLNameConflictsTracker.getClassifierRedeclarations` →
`LLCangJieSourceSymbolProvider.computeClassLikeSymbolByClassId` 的 `require(context == null || context.isPhysical)`
（早前记录的 7 类 × 3 = 21 失败即此）。该路径与本次修复的调用解析链路无交集，本次未动它；
D-3 的验收（PREFER_SELF 解析返回非 null）不依赖它。

## 2.4 收尾：指令映射守卫与 PREFER_SELF 夹具路径打通（2026-10-08，随 D-3 修复）

6.8 修好后，2.4 欠的两件事一并补上。

**一、指令→模式映射变成永久守卫。** 原先「模式选择」是抽象测试类里的私有方法：
指令声明了却没人读正是 2.4 的原始缺陷形态，而它自己仍只能靠「跑一遍管线」间接感知。
现在映射收进 `DanglingFileFixturePreparation.resolutionMode(directives)`，与指令容器
`DanglingFileTestDirectives` 同文件——谁加指令，谁就看得见旁边必须有的消费分支；
`DanglingFileFixturePreparationTest` 直接钉三条：未声明落默认 IGNORE_SELF、
两条显式声明各归其位、**同时声明直接失败**（挑一个跑绿的夹具是有害的）。
这些断言不跑分析，因此不受任何解析缺陷影响。

**二、PREFER_SELF 夹具路径有了端到端用例。** 新增数据文件
`analysis/analysis-api/testData/components/diagnosticProvider/collectDiagnostics/danglingUnresolvedReferencePreferSelf.cj`
（首行 `// PREFER_SELF_MODE`），与既有 IGNORE_SELF 文件同形：唯一诊断是那个确实不存在的名字。
它本身不区分模式（两种模式下该诊断都一样，Kotlin 的同一目录也是两种模式共用一份 golden），
价值在于**把 PREFER_SELF + dangling 夹具这条路径真正跑起来**——此前 7 个 cfir 生成类与
6 个 standalone 生成类从没有过一个 PREFER_SELF 数据文件，早前那条
`require(context.isPhysical)` 全文件中断正是藏在这条无人走的路上。

生成侧注意：新增数据文件必须重跑生成器，否则 `testAllFilesPresentInCollectDiagnostics`
守卫会红。`--rerun` 只作用于**它前面紧邻的**那个任务，所以两个模块要分开跑：
`:analysis:analysis-api-cfir:generateTestGeneratorTests --rerun` 与
`:analysis:analysis-api-standalone:generateTestGeneratorTests --rerun`；
生成 diff 只含新方法（每个受影响的生成类 6 行）。golden 由框架在首次运行时报
「Expected data file did not exist. Generating: …」并写出，人工复核后重跑即绿。

**验证**：`:analysis:analysis-api-cfir:test` 定靶 85 用例 0 失败（7 个 dangling 生成类全量文件 +
守卫 + 夹具用例）；`:analysis:analysis-api-standalone:test` 定靶 62 用例 0 失败。

## 5.4 短名可达性绑定到位置作用域（2026-10-08）

设计 §6.6 的现状条目写的正是这条：「三态判定直接可达性只查文件 lookup scopes，
**不能替代任意位置的局部遮蔽**」。旧 `checkCompletionCandidate` 的实现只查文件级作用域
（本文件顶层、包、导入、内建），于是三件事都判不出来：

| 场景 | 旧行为 | 新行为 |
|---|---|---|
| 顶层同名符号被**局部变量**遮蔽 | DIRECT（错：短名在位置上已绑到别处） | HIDDEN |
| **局部**候选自身 | UNKNOWN | DIRECT |
| **成员**候选（隐式接收者） | UNKNOWN | DIRECT |

### cjc 取证（1.1.3，`--diagnostic-format json`）

```
let Foo = 3 ; let x: Foo = …   → sema_not_a_type    'Foo' is not a type
let call = 2 ; call()          → sema_no_match_operator_function_call
```

即仓颉的**类型名与值名共享同一套遮蔽**，不存在「值遮蔽不影响类型位置」的例外——
遮蔽判定因此不必按符号种类分情况，`'Foo' is not a type` 就是「短名被占了」的直接证据。

### 改法

`CaCfirImportPlanning.isDirectlyReachable` 换成 `resolveShortNameBinding`，返回
`UNBOUND / EQUIVALENT / SHADOWED` 三态，`checkCompletionCandidate` 据此把
SHADOWED 判成 **HIDDEN**（而不是 REQUIRES_IMPORT）：import 不参与遮蔽，
加一条 import 也改不了「短名已经绑到别的声明」这个事实。

- **位置层**（词法、类型/成员、片段顶层）从 `CaScopeContext.scopes` 取——它已按真实查找
  次序从内到外排列，**首次绑定即结论**，与 `CfirLocalScopeCompletionSection` 的
  「首个同名者胜」同源。
- **文件层**（本文件顶层/包/显式与默认导入/内建）仍走 resolver 共用的
  `CfirFileLookupScopes`，**不**从位置上下文取。原因是一个顺手查出来的既有缺口：
  位置上下文的公开投影只包装 names-aware 作用域（`toPublicScopeOrNull`），而
  `CfirImportScope`（显式/默认导入）与内建原始类型作用域**不是** names-aware，
  投影里根本没有这两层。第一版改法整段依赖位置上下文，直接把 `import sample.*` 的
  可达性判没了——12 个缩短类 15 个用例红（期望 DIRECT 实得 REQUIRES_IMPORT），
  正是这个缺口造成的。该缺口**未在本项修**（属公开作用域投影的独立问题），
  已在 `resolveShortNameBinding` 的 KDoc 里写明，避免下一个人再踩。
- 片段例外：宿主文件顶层声明那一层只存在于位置上下文里，因此片段位置一并消费
  位置上下文的文件级带（导入层仍由上面的 CFIR 通道补齐）。
- 局部声明的短名取 `callableId?.callableName ?: 声明名`：局部声明是 callable 但**没有**
  callableId，只按 callableId 取名字会把它们整体判成「没有短名」，遮蔽与命中就都看不见
  （第一版就撞在这里：断言拿到 UNKNOWN，排查后才定位到 `shortNameOrNull` 的 callableId 分支）。
- `CaCfirScopeProvider.scopeContext` 补**单条位置缓存**（键 = 位置身份 + 文件修改戳）：
  一次补全请求会对同一位置反复判候选，而位置投影每次都要跑一遍 ContextCollector，
  不加缓存主路径就变成 O(候选数 × 上下文收集)。
- 只按名查询（classifiers / callables / typeParameters），不枚举作用域全量声明：
  成员作用域与包作用域的全量枚举代价随被查类型/包规模增长。

### 验证

- 新增 `completionCandidateShadowing`（上表三例，正反同夹具）：`AnalysisApiCfirComponentExecutionTest` 6/0。
  旧实现下三条里两条必红，因此这是真回归用例而不是记录现状。
- 回归：`:analysis:analysis-api-cfir:test` **1649/0**、`:analysis:analysis-api-standalone:test` **719/0**、
  `:code-insight:completion:impl-cfir:test` **66/0**、`:lsp:test` **84/0**。

### 边界（未做，如实记录）

- **alias 名字的可达性不在本项**：`checkCompletionCandidate` 问的是「候选按**自己的**短名是否可达」，
  而 alias 是名字层概念（`import foo.Bar as Baz` 之后 `Bar` 仍需限定）；展示哪个名字由候选生成侧决定。
- **A4 适用性检查**（`CaCompletionExtensionCandidateChecker` / `CaExtensionApplicabilityResult`）仍是独立入口，
  本项只动可达性，不越界替它判适用性。

## 4.1/4.2/4.5 具体类型作用域与替换后签名（2026-10-08）

设计 §6.4 的现状条目：「公开 `CaType.scope` 将具体类型还原成 class-like，再调用 `unsubstitutedScope`，
**不能保证实际类型实参保留**」——`Box<Int64>` 的成员签名里仍是声明侧的 `T`。

### 4.2 抽接缝（含一个被实测发现的缺口）

`cfir/providers` 的 `CfirReceivers.kt` 里，`typeToScope`/`collectTypeScopes` 是唯一一处统一处理
「类型变量上界、交叉类型、ideal literal、primitive extend、class-like 类型」且**以完整 Cone 类型
（而非 class-like 声明）为 owner** 构造成员 scope 的地方（源码注释就写着「同一个泛型声明的不同实例
拥有不同的成员签名，不能只按 ClassId 去重」）。此前它是**文件私有**，现提为跨模块接缝并加
`@CfirImplementationDetail`，KDoc 写明「需要实际类型实参/上界/intersection/builtin extend/继承替换的
调用方都走这里，不要各自复制一份 scope 构造」。

**顺手修掉 typealias 缺口**：`collectTypeScopes` 不展开别名类型，别名类型的接收者会退到别名自身的
classId，于是成员取不到——实测 `type IntBoxAlias = Box<Int64>` 的类型作用域为 `null`。
按 `ConeTypeAliasType.expandedType` 递归展开后恢复。

**夹具拼写取证**：cjc 1.1.3 只接受 `type X = Y`；`typealias X = Y` 被 parse 直接拒绝
（`parse_expected_decl: expected declaration, found 'typealias'`）。仓内 `typealiasType.cj` 用的是后者
（前端比 cjc 宽松），写夹具时以 cjc 为准。另：cjc 要求可继承的类显式 `open`，
`class IntBox <: Box<Int64>` 会被拒（`sema_non_inheritable_super_class`），夹具已改为 `open class Box<T>`。

### 4.1 新入口

- 新增 `CaTypeScope`（对齐 Kotlin `KaTypeScope`：只暴露**替换后**的签名与 classifier，不暴露未替换的 `callables`）
  与 `CaTypeScopeProvider`（`val CaType.typeScope: CaTypeScope?` + `val CaTypeScope.declarationScope: CaScope`），
  接进 `CaSession` / `CaBaseSession` / `CaCfirSession`；实现走上面那条接缝（`USE_SITE` 种类），
  声明视图复用旧路径（`unsubstitutedScope`）作为对照。
- **旧 `CaType.scope` 的行为一字未动**，符合设计「先增加新入口……变更旧返回类型必须单独迁移全部调用者」。
- 两处偏差记录在案：①Kotlin 复用同一名字 `KaType.scope`，仓颉因旧入口占用而取名 `typeScope`；
  ②`CaCallableSignature` 目前**没有** `valueParameters`（Kotlin 有），替换后的值参数只挂在
  `CaFunctionSignature.valueParameters` 上——所以 4.5 的值参数断言走函数签名通道，补齐属后续。
- **D-6 触面**：新组件按既有形态补了两条手写 `context(session)` 桥接（那套标记「自动生成，请勿手工修改」
  的桥接**没有生成器**，见 7.5 的记录在案决策）——本项再次印证 7.5 的结论：新增成员必须手写桥接，
  可机械化的是「校验」而不是「生成」。
- API surface baseline：`analysis-api` 与 `analysis-api-impl-base` 的 dump 已重生成
  （新增 `CaTypeScope`/`CaTypeScopeProvider` 与 5.5 的 `CaCompletionCandidateStatus.shouldHide/isDeterminedUsable`），
  `checkApiSurfaceDump` 四模块全绿。

### 4.5 用例

`TypeScopeExecutionTest.typeScopeSubstitution`（夹具 `testData/typeScope/typeScopeSubstitution.cj`，经 cjc 校验）七例：

| # | 形态 | 断言要点 |
|---|---|---|
| ① | 泛型本类 `Box<Int64>` | `unwrap` 签名 `(Int64): Int64`，且 `symbol` 仍是 `Box.unwrap`（替换不换身份） |
| ② | **声明视图对照** | 同一成员在 `declarationScope` 里仍是 `T`——这是两个视图的分工处 |
| ③ | 泛型父类 `IntBox` | 继承链上的成员同样替换为 `Int64` |
| ④ | type alias | `type IntBoxAlias = Box<Int64>` 同样替换 |
| ⑤ | 完全实例化受约束 extend | `Box<Shaped>` 有 `shapedTag: Shaped`；`Box<Int64>` **没有**（约束不满足） |
| ⑥ | 属性 | `Container<Int64>.stored: Int64`，声明视图仍 `T` |
| ⑦ | 非 class-like receiver | 有上界的类型参数按其上界 `Shaped` 取到 `area` |

断言全部比**类型**而不是名字：名字在两种视图里完全相同，只比名字的断言抓不到「看起来对但其实没替换」这种失效。
「连续替换」不在本项重复——它由 4.3（已勾选）的用例承担。

### 未做（如实记录）

`CaCallableSignature.valueParameters` 的补齐、旧入口（`CaType.scope` / `instanceMemberScope` /
`staticMemberScope`）的调用方迁移，都是设计里明确「单独迁移」的部分，本项只加新入口。

### 验证

`TypeScopeExecutionTest` 1/0；回归：`:analysis:analysis-api-cfir:test` **1650/0**、
`:analysis:analysis-api-standalone:test` **719/0**、`:code-insight:completion:impl-cfir:test` **66/0**、`:lsp:test` **84/0**、
`:cfir:analysis-tests:test` **8965 中 44 失败**（与 D-3 修复后的基线**完全一致**，即接缝扩展既未新增失败也未修掉 LLT 失败）、
`:analysis:low-level-api-cfir:test` 136 中 2 失败（既有）、`:cfir:resolve:test` 137 中 1 失败（既有）。

**偶发（未归因）**：本节验证后的一次全量 `:analysis:analysis-api-cfir:test` 里，`CfirIdeNormalAnalysisLibraryBinaryModuleTypeScopeTestGenerated` 报过一次 `Symbol was not restored: CaCfirConstructorSymbol（sample.scope.User.<init>）`；该生成类单独重跑与整套重跑均绿（1653/0），判定为顺序/会话状态相关的偶发，不从本次类型作用域改动出发排查。


## 13.3/13.4 并行执行器与串并行等价（2026-10-09）

设计对并行的唯一要求写在 G4 准入行（「§7.8、完整 SMART、并行准入」），口径由 tasks.md 给出：
**独立 Analysis 会话 + 稳定归并 + 禁止共享裸会话状态**，且**通过准入前不得开启**。

### 实现（13.3）

`CompletionRunner` 接口补 `runParallel(...)`（默认实现退化为串行——「能不能并行」变成显式的实现声明），
新增 `ParallelCompletionRunner`：

- 阶段划分与阶段内次序**逐条照搬**串行（快速 → 延迟；`executionPriority` 升序、同优先级保持注册顺序）；
- 每个区段在自己的工作线程上执行、拿**自己的缓冲 sink**（区段之间看不到彼此的批次）；
- 归并在调用线程上按区段次序完成：第 i 个区段一结束就发它的批次，**不按完成先后发**——
  否则候选次序随调度抖动，与串行结果不再等价；
- **取消检查放在归并之后**：串行在区段执行时批次已经进 sink，用户继续输入不会收回已发结果；
  并行必须同义，否则同样的取消时机会让并行少给一批候选（第一版写成「先检查后归并」，被等价用例逼出来）；
- 失败/取消按执行次序等待，因此**最早**的那一个就是向外抛出的那一个；已提交区段 `shutdownNow` 中断、
  未归并批次丢弃、不发收尾（与串行一致）。
- 装配点 `createCompletionRunner()`：默认串行；`CangJieCompletionRunnerOptions.parallelEnabled`
  （系统属性 `cangjie.completion.parallel` 作为实验入口）**未通过准入前不得打开**。
- `CompletionSection` 的并发契约（不共享可变状态、不共享裸 Analysis 会话、同实例跨请求可能落在不同线程）
  写进接口 KDoc——这是区段作者必须遵守的部分，执行器只能保证自己不引入共享。

### 准入与用例（13.4）

`ParallelCompletionRunnerTest`（8 例）逐条对照串行：①区段跑在工作线程且互不同线程（显式给两个线程，
避免单核机器上误判）；②**后序区段先跑完仍按区段次序发出**（先断言完成次序确为 second→first，再断言归并次序）；
③**串并行批次与收尾信号序列完全一致**（含延迟阶段与同优先级）；④区段不共享 sink；⑤失败按原样传播、
后续批次一律不发；⑥**取消时机一致**（同一夹具分别跑串行与并行比较批次序列）；⑦空区段仍收尾一次；
⑧并行执行器的串行入口可用。

**未达成项（故保持默认关闭）**：真实工程上的冷/热耗时对比需要真实 SDK 规模（与 13.2 的预算同批）。

## 14.1 补全测试 DSL（2026-10-09）

移植**适用**的六个指令 `EXIST` / `ABSENT` / `NUMBER` / `NOTHING_ELSE` / `WITH_ORDER` / `INVOCATION_COUNT`，
新增 `PREFIX`（前缀写在请求上；缺它 `NOTHING_ELSE` 只能断言一个随无关候选漂移的集合）。
两处有意偏差：位置用既有夹具约定 `completionTarget` 引用名标注（Kotlin 的 `<caret>` 不是合法仓颉，
会让夹具无法分析）；断言跑在**真实服务**上而不是某个区段（用户看到的那份候选表由服务归并产生）。
夹具见 `impl-cfir/testData/completionDsl/`（6 个），用例 `CompletionDslTest` 6/0。

顺带锁住一条生产规则：`repeatedInvocationWidensCandidates` 断言「首轮 ⊆ 重复轮 且确实变大」——
索引区段在 `invocationCount > 1` 时按设计放宽匹配，**不点名具体多出哪个候选**（那会把用例绑死在某个 `.cjo` 版本上）。

为让 DSL 在无进度上下文里可跑，`CfirCompletionService` 的指示器取值补了回退：无头宿主/测试里退化为
**空指示器**而不是 NPE（取消能力由宿主提供，没有它不该把一次补全变成异常）。

**验证**：impl-cfir 72/0（15 类）、impl-shared 74/0（含 14 个 runner 用例）、lsp 84/0。

### 续：13.2 的 `resolve` 阶段耗时已接线（2026-10-09）

`CompletionStage` 补 `RESOLVE`（与 `CANDIDATES` **分开**记：resolve 是用户按下候选才触发的
第二次分析，耗时直接体现在「翻候选卡不卡」上，混进候选生成看不出来）。
计时在 `AnalysisApiCangjieAnalysisFacade.resolveCompletionItem` 用 try/finally 包裹整个实现——
**失败路径与所有 `skipResolve` 早退同样计时**（「没填文档」也花了时间，漏记会让面板上这项永远只有成功样本）。
用例 `LspCompletionStatisticsTest.resolveRecordsItsOwnStage` 断言该阶段存在（记录式 sink，不报错也发现不了漏埋点）。

13.2 **仍未勾选**：冷/热与大/小项目基线与预算需要真实 SDK 规模的工程（与 6.6 的耗时预算同批），
按设计「预算须写入具名测试配置文件后才可评估阶段门禁」，本工作区不编造数字。

## 14.4 窄集与跨层回归记录（2026-10-09）

按任务要求逐项留痕：命令、基线、结果、未覆盖项。**命令统一经队列包装器**
（`./gradlew-queue.bat`，参数原样转发）；所有结果以 `build/test-results/test/TEST-*.xml`
根节点的 `tests/skipped/failures/errors` 为准，不只看 `BUILD SUCCESSFUL`。

### 窄集（受影响模块）

| 命令（节选，`--rerun` 视需要） | 结果 | 说明 |
|---|---|---|
| `:analysis:analysis-api-cfir:test` | 1653 / 0 | D-3、2.4、3.8、4.x、5.4、5.6 的落点 |
| `:analysis:analysis-api-standalone:test` | 719 / 0 | 同一批夹具在 standalone 配置下的镜像 |
| `:code-insight:completion:impl-cfir:test` | 72 / 0 | 补全区段 + 14.1 的 DSL 6 例 |
| `:code-insight:completion:impl-shared:test` | 74 / 0 | 13.3/13.4 的 runner 用例 |
| `:lsp:test` | 85 / 0 | 分支 B 全链路（含 13.2 的 resolve 计时） |
| `:cfir:resolve:test` | 137 / 1 | 唯一失败是既有：`CfirMapArgumentsTest > Defaults > number of default arguments is counted`（停用本变更后同样失败） |
| `:analysis:low-level-api-cfir:test` | 136 / 2 | 既有：`CfirCjmpCallerFirstDiagnosticsTest` 2 个（D-3 修复前为 4 个） |

### 跨层回归

| 命令 | 结果 | 说明 |
|---|---|---|
| `:cfir:analysis-tests:test`（8965 项，约 17 分钟） | 44 失败 / 395 跳过 | **全部既有**（宏语料 + cjmp 诊断语料）。同过滤器 A/B：停用 D-3 的消解器时 40 个用例名失败、启用后 35 个——净修 5 个、无新增；4.x 的接缝扩展后仍为 44（无新增、也未再修） |
| `:analysis:analysis-api-cfir:test`（A/B 前） | 1629 / 0 | 5.5 基线 |
| `:cfir:raw-cfir:psi2cfir:test` | 686 / 0 | ✔ 2026-10-09 补跑（`CfirReceivers` 接缝的直接下游） |
| `:cfir:raw-cfir:light-tree2cfir:test` | 262 / 0 | ✔ 同上 |
| `:cfir:cfir-serialization:test` | 115 / 0 | ✔ 同上 |
| `:analysis:stubs:test` | 8 / 0 | ✔ 同上（此前记录过的 `BuiltinsStubsTest` 差异当前不复现） |

### 未覆盖项（如实记录）

1. ~~最后两轮改动之后未重跑 `:cfir:analysis-tests`~~ —— **2026-10-09 已补跑**：
   `:cfir:analysis-tests:test` **8964 项、44 失败、395 跳过**，与既有基线完全一致（宏语料 + cjmp 诊断语料），
   即 14.1 的 DSL 夹具与 13.2 的 resolve 计时对 LLT 层**无影响**——不再靠「推测不受影响」支撑。
2. **偶发（未归因）**：一次全量 `:analysis:analysis-api-cfir:test` 里
   `CfirIdeNormalAnalysisLibraryBinaryModuleTypeScopeTestGenerated` 报过一次 `Symbol was not restored`
   （`sample.scope.User.<init>`）；单独重跑与整套重跑均绿，判定为顺序/会话状态相关。
3. **本工作区可跑的层已尽量跑满**：除窄集与 `analysis-tests` 外，还补跑了 `psi2cfir` / `light-tree2cfir` / `cfir-serialization` / `analysis:stubs` 四套（合计 1071 项、0 失败），覆盖 `CfirReceivers` 接缝的全部直接下游。**宿主侧回归不在本工作区**：`intellij-ide` 与 `deveco` 的插件加载/描述符唯一性/运行时插入
   （8.7、8.8、12.3–12.7、14.2、14.5、15.11、15.12）按分工由宿主侧会话执行。
4. **预算类指标未测**：13.2 的冷/热与大/小项目基线、6.6 的查询耗时预算需要真实 SDK 规模的工程，
   本工作区不编造数字（设计亦要求预算先落入具名测试配置文件）。

### 11.5① 展开映射的现状核查与决策（2026-10-09）

**结论：不实现，记录在案。**11.5 的两半——上下文分流与 `.cjo` 无源码/缺 SDK 降级——已落地并勾选项之外的部分
（见本项既有记录）；剩下的「宏展开后可映射代码的识别与可靠源映射」**不是本变更能补的缺口**，理由有三条，
都可在仓内验证：

1. **宏管线的产物按设计不进 CFIR。** `cfir/providers/.../macro/MacroSurface.kt` 的 KDoc 写明：
   「`MacroSurface*` **不实现** `CfirElement`，**不**进入 generated `cfir-tree` visitor / checker。
   任何把 surface 放进 provider-visible final CFIR 的尝试都应当被 `recordExpandedRawFilesOnce` 拒绝。」
   `recordExpandedFilesOnce` 的状态机（`RecordingState.EMPTY → OPEN_FOR_EXPANDED_RECORD → FINALIZED`，
   `CfirSourceSymbolProvider`）也正是围绕这条边界设的。要给出「展开后可映射代码」的分析层表示，
   先得让展开结果**成为可分析的 CFIR/PSI**，而那与这条硬性边界直接冲突——属于宏管线自己的路线决定，
   不该由补全变更顺手改掉。
2. **宏管线有自己的批次计划，且该计划引用的基线文档不在仓库里。** 代码多处引用「Baseline 第 2 节硬性边界 #7」、
   「第 7 节字段集合」、「Batch 4a / 4b」，但 `grep -rn "硬性边界|Batch 4a|Batch 4b" --include=*.md`
   在**工作树与主检出**都**没有命中**：计划只活在代码注释里。这既是「无法按原计划核对进度」的原因，
   也说明这条路线当前由宏侧自行推进（`MacroCallForest` / `MacroFragmentParser` /
   `TokenBackedMacroFragmentParser` / `MacroSymbolIndex` / `MacroDemandClassification` 一整套已就位）。
3. **本变更没有消费方。** 需要「展开映射」的 IDE 功能（在展开代码里导航/找定义）不在 G1–G5 任何准入项里；
   补全侧要的是「宏 token 位置不产出无关候选」，那一条已由 11.5 的分流用例锁住（手写全部区段清单，
   断言所有区段都不产出且不抛异常）。为没有消费方的能力造契约，正是本变更反复拒绝的做法（同 10.3）。

**留给宏侧的最小欠账（本项如实记录，不代为完成）**：把那份 Baseline 计划落进仓库，
否则「哪些批次承诺了展开映射」无从核对。

### 宿主面收尾与 15.10 定案（2026-10-10 ~ 10-11）

**结论：tasks.md 102/107。本工作区能闭环的全部闭环；余下 5 项见文末「未闭环项的准入」。**

**本轮提交**（工作树 `completion-k2-musing-bun`，分支 `claude/completion-k2-musing-bun`）：

| 提交 | 内容 |
| --- | --- |
| `6b0c0fab4` | 不稳定 dangling 副本的 Analysis session 自愈（LL 失效事件按设计不覆盖这类模块）；dummy 占位替换按入口分流（平台入口不写副本、无头入口「文档优先 + 自检 + PSI 回退」）；`cfirCompletionServiceOrNull` 的装配异常改 `LOG.warn` 不再静默 |
| `4036db61c` | `CjoStubAstConsistencyDiagnosticTest` 补「真实文件对账」（真实 `stubTree` vs `node` 的 stubbed spine）与 `textLength` 输出 |
| `52074b59b` | `LspIdeTwoEndPresentationTest`：同一条快照在 IDE 与 LSP 两端渲染的主文本/签名/类型求值结果一致 |
| `ddf5f62f4` | 阶段预算具名文件 + 覆盖守卫 + 冷/热小/大基线门禁（`LspCompletionStageBaselineTest`）+ 无 SDK 会话不扫描用例（`LspCompletionWithoutStdlibTest`） |
| `f61a03f32` | **解析器修复**：恢复 `parseImportDirectives` 里被注释掉的 `DoNotBindAnything` 注释绑定器——15.10「resolve 空文档」的真因 |

**关键定案一：15.10 的真因是解析器吞注释，不是「light PSI 没有 CDoc」。**
证据链（LSP resolve 路径逐层埋点）：`symbol.psiSafe` 非空、`file.isCompiled=false`、文件文本里有 `/** */`、
`PsiTreeUtil.findChildOfType(file, CDoc)` 为真；但 `declaration.docComment` 恒空，`documented` 的 AST 直接
前驱是 `IMPORT_LIST`，且 CDoc 的 AST 父节点落在 import list 里、PSI 父节点却是 `CjFile`——两棵树对不上。
真因：`psi/src/.../parsing/CangJieParsing.kt#parseImportDirectives` 里 `setCustomEdgeTokenBinders(DoNotBindAnything, null)`
被注释掉（其上方注释「允许注释绑定到首个声明」即用途），于是**没有 import 的文件**里 `package` 之后的文档注释
被绑定进 import list 子树。恢复后 resolve 端到端填出 `被记录的声明。\n\n第二段说明。`，
验收用例由 `resolveCannotFillDocumentationInThisContainer`（记录空文档）改为正向的
`resolveFillsDocumentationForDocumentedCandidate`。回归：`:lsp:test` 0 失败；`:psi:test` 96 例中 1 例失败，
且**临时撤回该改动后在同一断言（`ForeignAndAnnotationParsingTest.kt:137`）同样失败** ⇒ 既有基线。

**关键定案二：宿主的 stub/AST 不一致（14.2 原「库侧候选不可用」）是瘦身夹具缺模块。**
宿主夹具只装 `std.cjo`/`std.core`/`std.ast`/`std.objectpool`，而 `std.ast` 大量引用 `std.collection.ArrayList`：
缺它时反编译文本在类型位置渲染出 `<error>`（两边文本转储 diff：宿主 235181 字符 vs 上游完整夹具 240482，
142 处差异全是这一种），AST 与 compiled stub 树节点数因此对不上（46489 vs 47881）。
修法：`CangJieStdlibLightProjectDescriptor` 补 `std/std.collection.cjo`——顺带把宿主
`CangJieCompiledFilesHighlightingTest` 里同一条断言的**既有失败**转绿。
**宿主三处改动未提交**（本会话 git 守卫拦跨工作树操作）：上述夹具、`CangJieCompletionRegistrationTest.kt`
（真实入口用例 + 两条记录式探针）、`PlatformMatrixDeclarationTest.kt`（新增）。

**关键定案三：宿主 light fixture 的两条限制（14.2 ①撤销、②库侧候选）已用对照实验证伪「产品缺陷」。**
①撤销：对照实验里普通 `WriteCommandAction` + `commitDocument` 的文档改动，`ACTION_UNDO` **同样不回退**
（`CJD-UNDOCTRL commandEdit changed=true reverted=false`），且 `isUndoAvailable(fileEditor)` 在补全**之前**已为 true
⇒ 夹具的撤销栈被夹具自身命令占用，不能作为补全撤销语义的证据。②库侧：`cfir.index` 区段确被执行
（埋点 `CJD-IDX all=0 prefix='Str' kind=TYPE claimed=1`），但该夹具的 std 库不提供按前缀发现声明的能力，
各过滤点均未触发。两条的真机复验归 14.5。

**本轮跑过的验证**（均为实跑结果，命令见各任务行）：宿主 `CangJieCompletionRegistrationTest` 7/7 运行通过、
`PlatformMatrixDeclarationTest` 通过、`buildPlugin` 产出 `intellij-cangjie-2.1.4.zip`；
上游 `:lsp:test`（含新增 4 例）0 失败、`:analysis:analysis-api-cfir:test`、`:analysis:stubs:test`、
`:code-insight:completion:impl-shared:test`、`:code-insight:completion:impl-cfir:test` 全绿；
`:psi:test` 96 例 / 1 例既有基线失败。

### 未闭环项的准入（2026-10-11：5 条，均为「已决定的缺口 / 他分区工作 / 真机准入」，不是未完成的实现）

| 任务 | 准入 | 谁做 |
| --- | --- | --- |
| 6.6 持久化符号索引 | ①③④已落地；**②（IDE 侧跨会话持久实现）属宿主装配面**，且查询耗时预算须在真实 SDK 规模下测得，本工作区不编造数字。**2026-10-11 核查（同日修正）**：起初按 `grep -rl "FileBasedIndexExtension|StubIndexExtension|..."` 在上下游**均无命中**判定「没有可挂索引」，随后在 `psi/src/.../stubs/elements/StubIndexService.kt` 发现**写侧接缝已经预留**：该类是全空实现的开放类（每个 `indexXxx` 都空），而每个 stub 元素类型的 `indexStub(stub, sink: 平台 IndexSink)` 都已经调用 `getInstance().indexXxx(...)`，取用端 `?: NO_INDEX` 兜底；全仓缺的只是「索引键与扩展 + 写侧实现 + 读侧实现 + 四处注册」。**实施方案已出**：`design-6.6-persistent-symbol-index.md`（键设计、写侧/读侧、注册与版本升级规则、S1–S8 步骤、等价性/增量/持久/预算验证、extend 与嵌套的钉死用例、回滚路径）。 | 宿主侧会话 |
| 10.3 参数匹配权重 | 七维中六维已落地；「参数匹配」设计只提过一次、仓内无「期望参数」契约，**刻意不臆造排序规则**（等 A4 `CaCompletionExtensionCandidateChecker`） | 待 A4 |
| 11.5 宏展开映射 | 已决策「不实现，记录在案」（三条仓内可核理由见上节）；欠账是宏侧把 Baseline 计划落进仓库 | 宏侧 |
| 14.5 真实 sandbox | 需真人 GUI 会话在真实 IDE 里跑分发插件并留可复查记录（14.2 的撤销与库侧候选两条也在此复验）；静态面已核：ZIP 内 `since-build=253 until-build=253.*` | 变更负责人 |
| 14.6 默认开关 | 按其自身文字，须等 14.1–14.5 全部准入通过（含 14.5） | 14.5 之后 |

### 6.6② 持久化符号索引：S1–S4 实施与实测（2026-10-11）

**结论：S1–S4 已实现并端到端通过；6.6 仍未勾选（S5 等价性 / S6 增量与持久 / S7 预算未做）。**

**落点与文件**（方案 `design-6.6-persistent-symbol-index.md`）：

| 件 | 位置 |
| --- | --- |
| 四个索引键 + `VERSION` | `intellij-ide/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/index/CangJieStubIndexKeys.kt` |
| 四个 `StringStubIndexExtension` | 同目录 `CangJieStubIndexExtensions.kt` |
| 写侧（接前端已有的 `StubIndexService` 空钩子） | 同目录 `CangJieStubIndexService.kt` |
| 读侧（`CaSymbolIndexQueryService` 实现，`PERSISTENT`） | 同目录 `CangJieSymbolIndexQueryService.kt` |
| 注册（`stubIndex` ×4 + `applicationService` + `projectService`） | `modules/ide/base/src/main/resources/META-INF/cangjie-decompiled.xml` |
| 端到端用例（5 例） | `product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/index/CangJieSymbolIndexQueryTest.kt` |

**实测**：`:product:idea-plugin:test --tests "*CangJieSymbolIndexQueryTest*"` → **5/5 通过**，覆盖：
未打开文件里四类顶层声明可按前缀召回（种类正确）；嵌套类与 extend 成员不入索引；
枚举构造器条目带 `ownerClassId`（分析层靠它定位所属枚举，缺了会静默丢弃）；
>3 字符前缀走子串口径且**区分大小写**；查询计数与键扫描量有观测。

**实施中发现的两条事实**（已写进代码注释）：
1. **枚举构造器的父桩是枚举体**（不是枚举桩），因此「所属枚举顶层」必须向上找最近的类型声明桩再判；
2. **过滤器区分大小写**（契约的 `CaSymbolIndexNameFilter.matches`）：`Alpha*` 不会被 `"alpha"` 命中——
   用例最初就踩了这个，读侧返回的条目本身是对的。

 **（2026-10-11 续：S5–S7 补齐，6.6 勾选）**读侧补 `queryNanos`；宿主用例扩到 8 例全绿（新增 scope 限定、增量可见、扫描量上界、耗时记录四项）；语义等价性取「两端各自锁定同一口径」（standalone 侧 `CaStandaloneSymbolIndexQueryTest` ↔ 宿主侧 `CangJieSymbolIndexQueryTest`）；跨会话持久由机制保证（平台持久索引），真机复验记在 14.5。

（原记录）仍未做**：S5 与无平台实现（`CaStandaloneSymbolIndexQueryService`）在同一 filter+scope 下的等价性断言；
S6 增量更新与跨会话持久的实测证据；S7 键扫描量预算（接 13.2 的预算文件）。

## 2026-10-11：14.5「授权运行时」——最终制品的平台核验 + 撤销项独立复验

**可机械化的一半跑完了**（完整记录：`verification-14.5-plugin-verifier.md`，与主检出同步）。

### 1. JetBrains Plugin Verifier（制品 × 真实平台）

宿主 `build-logic/src/main/kotlin/cangjie.intellij-product.gradle.kts` 新增 `pluginVerification { ides { ide(ideToRunType, ideRunVersion) } }`，
被测平台与声明矩阵同源（IU-253.29346.50）。对 `intellij-cangjie-2.1.4.zip` 跑 verifier 1.410（20 m 50 s，另下载依赖插件 1.17 GB），
判定 **Compatible**：14 项 scheduled-for-removal / 168 项 deprecated / 265 项 experimental / 104 项 internal /
2 项 non-extendable，另有 3 条 plugin structure warnings；`Dynamic Plugin Eligibility: 可启停无需重启`。
报告：`product/idea-plugin/build/reports/pluginVerifier/IU-253.29346.50/report.html`。

三条结构告警是「描述符声明 + 依赖插件集里同样有」被判为重复依赖（`org.toml.lang` / `com.redhat.devtools.lsp4ij` / `com.intellij.modules.json`）——
**不能照字面删**：lsp4ij 是运行时必需且不随 IDE 分发的外部插件，删了加载面直接变红。按「需一次 runIde 冒烟再改」的打包决策处理，本轮不动描述符。
另有一条非制品问题的下载告警（`org.jetbrains.plugins.clion.radler` 下载 11 m 19 s 后 closed，verifier 记 failures 后继续）。

### 2. 撤销项：物理文件 + 真编辑器，四变体全不回退

14.2① 原先只在**内存 light 文档**上观测到「撤销不回退」。本轮换成**磁盘真实文件**（`tempDirFixture.createFile` +
`configureFromExistingVirtualFile`）并加**对照组**（普通 `WriteCommandAction` 编辑），触发各给两种
（`ACTION_UNDO` / `UndoManager.undo(FileEditor)`，后者经 `FileEditorManager.getAllEditors` 取 `FileEditor`——
`EditorImpl` 不是 `FileEditor`，直接强转会 CCE）：

| 编辑 | 触发 | 结果 |
| --- | --- | --- |
| 对照组（普通命令内编辑） | `ACTION_UNDO` | 不回退 |
| 对照组 | `UndoManager.undo` | 不回退 |
| 补全接受候选 | `ACTION_UNDO` | 不回退 |
| 补全接受候选 | `UndoManager.undo` | 不回退 |

⇒ **该夹具对任何命令内文档改动都撤不回来**（与载体、改动种类、触发方式都无关），故 14.2① 的判定维持：
这是夹具限制而非补全缺陷；真机撤销验收归 14.5 的 GUI 会话。
随后把这条夹具事实**钉成两条通过用例**（`CangJieCompletionUndoOnFileTest`）：撤销触发后文本必须仍停在编辑后；
将来平台或夹具修好撤销时它们会失败，届时翻成「撤销后逐字回到补全前」的真验收。

### 3. 仍未闭环（要真人 GUI 会话）

14.5 的**触发 / 排序 / 插入 / 撤销 / 错误态**在真实 sandbox 里的验收（`runIde`），以及 14.2 的第二条夹具限制
（库侧候选按前缀发现）。verifier 只覆盖加载/结构/兼容面，替代不了 GUI 会话；故 14.5 仍不勾选。

## 2026-10-11（真机验收驱动）：图标按声明形态、泛型插入决策翻转、10.3 与 11.5 收口

### 1. 真机沙箱查出并修复的两条（补全自身的缺陷）

产品侧在 `runIde` 沙箱里实测报了两条：**候选图标全是 class 图标**、**`A<T>` 补全后只剩 `A`**。

- **图标**：`LookupIcons.iconOf` 原先只按 `CompletionItemKind` 取图标，而顶层一类里同时有
  class / interface / struct / enum / typealias / 函数，于是它们共用 `AllIcons.Nodes.Class`（成员也一律 Method）。
  修法：契约新增 `CompletionSymbolKind`（**声明形态**，与「候选类别」分工：类别决定排序与插入形态，形态只管图标），
  `CaSymbol.completionSymbolKind()` 由符号类型判定（`CaClassKind` → class/interface/struct/enum、`CaTypeAliasSymbol` → typealias、
  构造器/枚举构造器/属性/字段/变量/形参/类型参数各自映射），映射改为 `iconOf(kind, symbolKind)`；
  **形态未定按类别兜底**（引入前行为），漏标只会少一次精细化、不会空图标。
- **泛型插入**：原先（11.1 的决策）刻意不插 `<`，`<T>` 只进尾文本；真机实测「补出 `A` 之后用户无从知道要写几个类型实参」，
  **决策翻转为插入**：契约加 `CompletionInsertion.typeParameterList` 与 `CaretPolicy.INSIDE_NEW_TYPE_ARGUMENTS`，
  插入 `Box<T>`（caret 落在 `<>` 内），构造位置是 `Box<T>(`（caret 仍在 `<>` 内，类型实参在语法上先于调用实参）；
  用户已写 `<` 或 `<...>` 时**绝不重复插**，`<` 未闭合时不补括号。插入仍走 `performCangjieInsertion`
  ——**IDE 与 LSP 共用同一份实现**，因此 LSP 侧编辑自动一致，并补了 LSP 用例。

### 2. 10.3（`completion-parameter-matching-weight`）完成——并更正立项前提

立项时写的「分析层没有从实参位推导期望类型的代码」**是错的**：`CaCfirExpressionTypeProvider.getExpectedTypeOfFunctionParameter`
早就实现了（`CjValueArgument` → 所在调用 → `valueArgumentMapping[实参].returnType`），且被生成套件的
`expectedExpressionType/functionArgument.cj` 覆盖。立项时的 grep 用错了词（找 `argumentIndex`/`valueArguments`）。
本轮实际交付：契约文档补「实参位」上下文（含位置/命名实参两种绑定），新增 `AnalysisApiCfirExpectedParameterTypeTest`
（位置实参取类型 ✓、**命名实参按名绑定**用「写序与形参序相反」的夹具区分、形参名不存在 → 未知 ✓）。
**变长参数位给出「未知」**（理由与判定见第 4 节；最初写的「尾随数组形参被误判」已被取证更正）。
**夹具坑两条**（写用例时撞到，已记进用例注释）：仓颉的命名参数要在**声明侧**用 `!` 标注；「实参多于形参」的夹具会让
`resolveToCall` 在诊断转换处抛 `ClassCastException: CjOffsetsOnlySimpleDiagnostic -> CjPsiDiagnostic`（既存缺陷），故该用例缺位并写明。

### 3. 11.5（`macro-expansion-source-mapping`）完成

- 契约：`analysis-api` 新增 `CaMacroExpansionMapping`（`None` / `Ambiguous` / `Inside(surfaceId, start, end, reliability)`）
  与三态 `CaMacroExpansionReliability`；**不暴露 `MacroSurface` 本身**，边界不变。入口两个：`PsiElement.macroExpansionMapping` 与
  `macroExpansionMappingAt(file, offset)`。
- 实现：`cfir/providers` 新增 `MacroSurfaceLookup.kt`（`MacroCallForest.lookupSurfaceAt`：闭区间、取最内层、歧义如实上报；
  与 `MacroCallNode.incomingChannel()`）；`analysis-api-cfir` 新增 `CaCfirMacroExpansionMappingProvider`，
  从 raw builder 的 surface 存储按 PSI 文件身份取表（与 `CfirImportsChecker` 同源）并建森林。可靠性只看父边通道：
  `ATTR`/`INPUT` ⇒ 可靠，`UNRESOLVED` ⇒ 不可靠，root ⇒ 可靠。
- 用例：`MacroSurfaceLookupTest` 8 例（嵌套取最内层 / 端点归属 / 跨行 / 无展开 / 重叠不嵌套报歧义 / 无 range 不参与 / 通道来自父边 / 未覆盖通道 ⇒ UNRESOLVED）。
  消费侧**未改任何区段**：宏位置本来就由位置分流挡成「所有区段不产出」，新增用例
  `macroTokenPositionIsInsideReliableExpansion` 把「映射判为 `Inside(RELIABLE)`」与「索引候选为空」钉在同一个用例里——
  只验其一都会得出错误结论。

### 4. 变长参数判定：更正前提并修掉真缺陷

最初我以为 `isVararg` 的「最后一个形参且类型是 Array」是**启发式**、会把正常的尾随数组形参误判。
取证后**更正**（证据：官方指南的变参糖说明、官方编译器 `Sema/TypeCheckCall.cpp#IsFuncDeclPossibleVariadic`、
cjc 1.1.3 实测 `func sum(arr: Array<Int64>)` 可 `sum()`/`sum(1,2,3)`/`sum(arr)`）：
**仓颉没有 `vararg` 语法记号**，「变长参数」就是「最后一个**非命名**形参是 `Array<T>`」这条调用语义规则
（`...` 只用于 C FFI 的 `hasVariableLenArg`，且已被 raw builder 从 `valueParameters` 剔除）。

真缺陷是另外两条：
1. 分析层用的是**整张形参表的最后一位**，而不是「最后一个非命名形参」——
   `func collect(arr: Array<Int64>, extra!: Int64)` 现在判 `false`，按官方应当 `true`（`collect(1,2,3, extra: 4)` 合法）；
2. 缺两个官方例外（enum 构造器不可变长；除 `()`/`[]` 外的运算符重载不可变长），
   且判定与 `cfir:resolve` 的 `CangjieVariadicUtils` 重复、已经漂移。

修法：把判定收成一份——`CfirDeclaration.cangjieVariadicParameterOrNull()`（`cfir:resolve`）作为唯一出口，
`CaValueParameterSymbol.isVararg` 直接委托；分析层的期望类型在变长位返回「未知」
（该位可传数组本身或 0..n 个元素，无统一期望）。回归用例：
`arrayParameterBeforeNamedParameterIsVariadic`（旧实现此处会误返回 `Array<Int64>`）、
`variadicArgumentYieldsNoExpectedType`。**未覆盖**：enum 构造器与运算符两个例外尚无用例。

**本轮提交**（本工作树 `claude/completion-k2-musing-bun`）：
- `3bbeb6a14` feat(completion)：候选图标按声明形态、泛型类型插入类型参数列表（真机查出的两条）。
- `e5b1cf89b` feat(analysis,cfir)：宏展开源映射的只读契约与实现（11.5）。
- `1d6464a16` fix(analysis,cfir)：实参位期望类型收口——变长判定统一、变长位不给期望类型（10.3）。

**验证**（本轮全绿）：`MacroSurfaceLookupTest` 8、`CfirMacroTokenPositionTest` 2、
`AnalysisApiCfirExpectedParameterTypeTest` 5、`CangjieInsertionTest`、`LookupElementFactoryTest`、
`LspCompletionEditCalculatorTest`。

**2026-10-11 续（10.3 的 1.3 定位 + 运算符例外用例）**
- `b7534edf8` test(analysis)：`()` 运算符允许变长改走**真实调用**断言。
  原先直接查声明符号的 `isVararg`，实测发现那会因为**形参类型未解析**而一律 `false`——
  `+` 那条会因错误的原因通过、`()` 那条直接失败；改成 `callable(1, 2, 3)` 后取实参位的期望类型，
  才真的在判别运算符例外。**未覆盖**：除 `()`/`[]` 外的运算符不得变长这一支（二元写法拿不到实参映射，已记录）。
- **10.3 的 1.3 从「未区分」升级为「已定位到段」**：命名实参调用**能解析**（1 条 `CaBaseSimpleFunctionCall`）、
  PSI `isNamed()==true`，但 **`valueArgumentMapping` 为空** → 空缺在「CFIR 实参映射 → 公开映射的投影」段
  （`CaCfirResolver.buildValueArgumentMapping` 取键或取下标时静默跳过）。用例保留为「缺口记录」。
- **14.5 的 GUI 面分工已定**：由用户自建 UI 机器人（`runIdeForUiTests` + `remote-robot`）打通后出证据；
  本工作区留好 verifier 报告、`verification-14.5-plugin-verifier.md`、`real-machine-checklist-14.5.md`，
  并核实了两条基建事实（253 分发包**不自带** Robot Server；宿主 IPGP 为 2.10.5）。

**2026-10-11 续二（10.3 的 1.3 真正修好）**
`774fe2dbc` fix(analysis)：命名实参进公开实参映射——投影时拆到值表达式。

- 现象：`CaFunctionCall.valueArgumentMapping` 对**命名实参**恒为空（调用能解析、PSI `isNamed()==true`，映射 size 0）。
- 根因：CFIR 的实参映射对命名实参给的是 `name: value` **节点**，其 PSI 是 `CjValueArgument` 而不是表达式，
  投影时 `as? CjExpression` 落空后 `continue` **静默跳过**（只影响命名实参；位置实参不受影响）。
- 修法：投影前先拆到值表达式（`CfirNamedArgumentExpression.expression`），与 `CfirMapArguments` 里 `CallArgumentInfo` 的口径一致。
- 结果：命名实参位能取到该形参的期望类型 → SMART 补全在这类位置可按形参类型过滤/加权；
  用例 `namedArgumentBindsByParameterName` 从「缺口记录」翻成**正向断言**并绿。
  未覆盖：乱序写法在本解析器下整条调用不解析（实测），故「按名而非按位次」的判别仍只有反例。

**过程教训（写下来免得再犯）**：这一轮出现过一次「自我验证的空转」——
两条编辑因用了**相对路径**（当时 shell 的 cwd 已漂到 `build/test-results/test`）没有落到文件上，
而随后的测试跑的是**旧文件**、自然通过，我据此报了「修好了」。
后来核 `mtime` 与文件内容才发现。**规则**：①编辑一律用**绝对路径**并当场回读校验；
②读测试结果前先核 `mtime` 是否属于本轮，别把上一轮的 XML 当本轮结论。

**2026-10-11 续三（图标修复的后续 + 1.3 的最终提交）**
- `774fe2dbc`：命名实参进公开实参映射（10.3 的 1.3 真正修好，见上节）。
- `5e1bd01be` fix(completion)：**内建类型落 OTHER 形态**。`completionSymbolKind` 直接读 `classKind`，
  而内建类型符号（`Int64`/`String`）该 getter 抛 `Unsupported class-like symbol` → 补全到内建类型时
  整条候选计算被打断。**这是 3bbeb6a14 引入的回归**，由区段级集成用例 `CfirInsertionIntegrationTest` 抓到——
  教训：图标/展示类改动也要跑区段级集成用例，只跑工厂与插入的单元用例抓不到。

