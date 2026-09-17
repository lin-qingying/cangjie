# cangjie 仓库：已确认的架构事实（详细版）

> `MEMORY.md` 的详细展开。每条都经过源码实测，用于**避免重复调研**。
> 结论若与旧记录冲突，以本文为准（含"⚠️ 更正"标记）。

## 1. 解析器（psi）

- **解析入口是文件感知的**：`CangJieParser.Companion.parse(builder, psiFile)`（`psi/.../CangJieParser.kt:147`），
  由 `CjFileElementType.doParseContents` 传 `psi.containingFile` 驱动（`CjFileElementType.kt:97`）。
  ⇒ 想"按文件类型改变解析模式"，**不需要新建 Language/ParserDefinition**，在 `parse` 里按 `psiFile` 分支即可。
- **解析模式机制**：`AbstractCangJieParsing.ParsingContext` 是 data class，15 个字段（`strictMode` 已注释掉）、
  **11 个预设，实际只用到 2 个**（`DEFAULT` 6 次 / `ANNOTATION_ONLY` 1 次），
  其余 9 个零使用点（`LEGACY`/`REPORT`/`SILENT`/`IF_WHILE_CONDITION`/`MATCH_EXPRESSION_MODE`/
  `FUNCTION_LITERAL_BLOCK`/`FUNCTION_LITERAL_COLLAPSED`/`MACRO_BACK_TOKEN`/`NO_STRING_INTERPOLATION`）。
  ⇒ 真实职责只是「给文件级入口选一套文法」。
  **不要往里加"文件语义模式"字段**（如 `isDeclarationFile`）：每个预设都是全新实例，任何局部 `with` 都会静默清除
  文件级属性；且分类维度混淆。正解见 v3 文档 4.2.3/4.2.5：模式作为 `CangJieParsing` 的**构造属性**。
- 文件级入口是「**一个入口 = 一个硬编码 `ParsingContext`**」：`parseFile()`（no-arg，`with(DEFAULT)`，`:1310-1327`）、
  `parseOnlyAnnotationFile()`（no-arg，`with(ANNOTATION_ONLY)`，`:1280-1297`）。
  **P1 已确认这两个 `with` 不需要改**：声明模式不进 `ParsingContext`（见下条）。
- **`CangJieParsing.sourceKind`（构造属性）是解析模式的唯一载体**（P1 已实现）：
  `class CangJieParsing private constructor(builder, isTopLevel, isLazy, initialLanguageModuleName,
  initialSourceKind: CjSourceKind = CjSourceKind.SOURCE)`；companion 的 `createForTopLevel` /
  `createForTopLevelNonLazy` / `createForByClause` 都多一个 `sourceKind` 参数（**默认 `SOURCE` ⇒ 既有调用点零改动**）。
- **缺体诊断的唯一抑制点**：`CangJieParsing.reportMissingBody(report: () -> Unit)` + 一个默认文案重载。
  `if (sourceKind.isDeclaration) return; report()`。改完后 `parsing.error.function.body.expected`
  **全文件只剩 1 处**（就在默认重载里）—— 若出现第 2 处，说明有人绕开了统一入口。
- **`parseMacro` 的分派包在局部 `with` 里**（`CangJieParsing.kt:2568-2571`）：
  `with(parseContext.copy(disableMacroParsing = false, ...)) { parser.parseMacro() }` ——
  `copy(...)` **整体替换**上下文对象。这是"模式不能放进 `ParsingContext`"最直接的现场证据：
  存进去会被静默清掉、无任何信号。
- **`macro` 在文件首位有歧义**：`macro package <name>` 是合法文件前导，`parsePackageDirective`
  （`CangJieParsing.kt:697-703`）会把**处于文件首位**的 `macro` 当宏包声明头消费掉，
  之后报 `Expecting 'package' keyword`。⇒ 要触发 `parseMacro` 的宏声明分支，
  `macro` 前面必须有内容（注解或 package 行）。写 `parseMacro` 相关用例时注意。
- **`CangJieLightParser` 没有上下文透传**：`parseWith`（`:63-79`）直接 `cjParsing.parseAction()`。
  **P1 已给 `parse` / `parseAnnotationOnly` 各加 `sourceKind` 参数并透传**；
  `LightTree2Cfir.buildCfirFileWithSurfaces(code, sourceFile, …)` 传 `sourceFile.sourceKind`。
  它自带 `reportErrors` 扫 `ERROR_ELEMENT`：模式不对会让无体成员的 `FUNC`/`PROPERTY` 节点结构不完整，
  进而让签名键算错、产生**静默**的匹配失败（不报错）。
- `CangJieParser.Companion.parse(builder, psiFile)` **P1 已改为对 `sourceKind` 的穷尽 `when`**（无 `else`），
  删除了 `name.endsWith(".cj.macrocall")` 这个第二真源；非 `CjFile` 兜底改用
  `CjSourceKind.fromFileName(psiFile.name)`（保留原"按名字识别"能力）。
  实测：宏调用文件走**独立的** `CangJieMacroCallParserDefinition`，其 `createFile` 返回
  `CjMacroCallFile`（`sourceKind` 覆写为 `MACRO_CALL`）⇒ 原后缀兜底本来就是冗余的。
- **`parseOnlyAnnotationFile()` 有两个不同身份的调用方**：① `.cj.macrocall` 文件级入口；
  ② 宏展开**片段重解析**入口（`CangJieLightParser.parseAnnotationOnly`，`LightTreeRawCfirDeclarationBuilder.kt:1477`）。
  后者与文件种类无关 ⇒ "按文件种类收敛"**只适用于文件级入口**，片段级必须保持显式。
- **`parseInitFunctionBody` 的 `at(COLON)` 分支是恢复逻辑**（`val error = mark(); while (!at(LBRACE)) advance(); error.error(...)`）。
  在 `.cj.d` 里没有 `{` 可恢复 ⇒ 循环会 advance 到 EOF **吞掉文件其余内容**，
  且 `mark()` 无 done/error 破坏标记平衡。P1 的处理是**整块早退**（消费返回类型后 return），
  不是"包一层 if 抑制 error"。**推论：凡抑制点同时伴随恢复动作，必须按早退处理。**
- **PSI/Stub 的 body 可空已完备**：`CjDeclarationWithBody.bodyExpression: CjExpression?`、`hasBody()`，
  stub 已序列化 `hasBody`/`hasBlockBody`。函数体就是 `CjBlockExpression` 子节点，**不存在 `FUNC_BODY` 节点类型**。

## 2. CFIR

- **P2 已实现（护栏 1/2）**：`isImplicitAbstractClassLikeMember/Function` 仅函数分支加 `!sourceKind.isDeclaration`；
  `DEFAULT` 与 `ABSTRACT` 同源（取反共享结论，官方 `SetDefaultFunc` 读的就是 `CheckFuncBody` 的结果）；
  属性侧 `DEFAULT := interface && !abstract && !implicitAbstract`（官方 PROP_DECL 同样"见 ABSTRACT 即 return"）。
  PSI 侧经 `containingFile as? CjFile` 回指取种类（无构造参数）；LightTree 侧是构造属性。
- **接口在 CFIR 中是 `CfirInterface`（`CfirClassLikeDeclaration` 子类），不是 `CfirClass`**：
  `filterIsInstance<CfirClass>()` 找接口成员得空集；class-like 断言/遍历一律用 `CfirClassLikeDeclaration`（其 `declarations` 持有成员）。
- **CFIR 的 body 可空已完备**：`CfirFunction.body: CfirBlock?`；PSI 与 LightTree 路径无体时都已返回 null
  （`PsiRawCfirBuilder.kt:3555`、`LightTreeRawCfirDeclarationBuilder.kt:3082`）。
- **CFIR 侧读"文件种类"的现成路径**：`CheckerContext.containingFileSymbol: CfirFileSymbol?`（`CheckerContext.kt:79`）
  → `CfirFileSymbol.sourceFile: CjSourceFile?`（`cfir-tree/src/.../symbols/CfirBasedSymbol.kt:748-753`）
  → `CfirFile.sourceFile`。**不要用会话级配置或 `CfirSession.Kind` 代替** ——
  `CfirSession.Kind` 是 `{Source, Library}`（"CFIR 从哪来"），与"文件是不是声明文件"正交
  （`cjc -d` 编译出的 `.cj.d` 也是 `Kind.Source`）。
- **属性位的隐式推导容易分叉**：`ABSTRACT` 与 `DEFAULT` 在本仓**各有一个独立谓词、都从 `hasBody()` 派生**
  （`PsiRawCfirBuilder.kt:3860/3872`、`LightTreeRawCfirDeclarationBuilder.kt:805-806`、`:959-960`）。
  官方是**同一函数体内顺序推导**（`ParseDecl.cpp:1518` 置 `ABSTRACT` → `:1529` `SetDefaultFunc` 读它 ⇒ `DEFAULT := !ABSTRACT`）。
  ⇒ 改动任一属性位时，**必须让它俩同源**，否则"`.cj` 下等价"的前提一旦被打破就静默分叉。
- **`.cj.d` 在 `class`/`interface` 体内的属性仍获得 `ABSTRACT`，函数不获得——但函数会额外获得 `DEFAULT`**
  （官方 `SetDefaultFunc` 无 `parseDeclFile` 门禁）。这个不对称是官方语义，不能"统一处理"。
- **`.cjo` 全部来自外部 `cjc`**，本仓不自产：`CjoPackageWriter` 除定义与单测外无调用点，且**不写 annotations**
  （schema 的 `Decl.annotations` 已支持，反序列化 `CfirDeclDeserializer.deserializeAnnotations:319` 已支持）。

### checker 框架（要改 checker 行为前必读）

- **没有 `CfirChecker` 这个类型**。真实基类是 4 个独立 `abstract class`：
  `CfirDeclarationChecker<D : CfirDeclaration>`、`CfirExpressionChecker<E>`、`CfirTypeChecker<T>`、
  `CfirLanguageVersionSettingsChecker`（均在 `cfir/checkers/src/.../checkers/*/`）。
- **分派处是生成代码**：唯一迭代点 `cfir/checkers/gen/.../DeclarationCheckersDiagnosticComponent.kt`
  的 `Array<CfirDeclarationChecker<E>>.check(...)`，文件头写着"**请勿手动修改**"。
  `DeclarationCheckers`（19 个槽位）**也在 `gen/`** 而不是 `src/`。
  生成器：`cfir/checkers/checkers-component-generator`（模板在 `Generator.kt:391-413`），
  由 `cfir/checkers/build.gradle.kts` 的 `generatedDiagnosticContainersAndCheckerComponents()` 挂载。
  ⇒ **要改分派行为，改生成器模板，不要手改 `gen/**`。**

## 3. 编译器驱动 / 配置

- **compiler/frontend 是未接通的骨架**：`AbstractFrontendPipeline` 无子类、`CfirFrontendPipelinePhase` 无调用方、
  `ArgumentsPipelineArtifact → ConfigurationPipelineArtifact` 桥接缺失。
  真正被使用的前端入口是 `cfir/entrypoint` 的 `analyse.kt`（`buildCfirFromCjFiles` / `buildCfirViaLightTree`）。
- **配置项两段式**：`CommonConfigurationKeys` 加 `CompilerConfigurationKey` + `CompilerConfiguration` 扩展属性。
  CLI 参数用 DSL（`compiler/arguments/.../compilerArguments.kt`）生成
  `compiler/frontend/gen/.../CommonCompilerArguments.kt`；
  生成任务在 `compiler/frontend/build.gradle.kts` 的 `generatedSourcesTask("generateFrontendArguments", ...)`。
- **⚠️ 更正：DSL 与生成物**没有**漂移**。`useFir` 是 `CommonConfigurationKeys.kt` 的**配置扩展属性**，与 CLI 参数无关。
  生成物属性数 = DSL 声明数 + 生成器合成的 2 条（`additionalSyntheticArguments = autoAdvanceLanguageVersion,
  autoAdvanceApiVersion`，见 `frontend-arguments-generator/.../Main.kt`）。
- **`CangJieCompilerArgument.compilerName` 用于指定生成类中的属性名**，且因 `?:` 绑定优先级低于 `.`，
  `calculateName()` 里的 `removePrefix/split/joinToString` **只作用于 `name` 分支** ⇒ `compilerName` 原样用作属性名。
- PLATFORM 事实：`AbstractFileViewProvider.getFileType()` 是 **final** 且返回 `myVirtualFile.getFileType()`
  （`SingleRootFileViewProvider` 不覆写）⇒ `viewProvider.fileType` ≡ `virtualFile.fileType`。
  `.cjo` 的 FileType 是 `CangJieBuiltInFileType`（XML 注册：`intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-filetypes.xml`；
  `psi/resources` 下只有 `messages/*.properties`，**没有** META-INF）。
  PSI 文件的创建点（全仓）：`CangJieParserDefinition.kt:110`、`CangJieMacroCallParserDefinition.kt:116`
  （`CjMacroCallFile`）、`CjCodeFragment.kt:53`、`analysis/decompiled/.../CjDecompiledFile.kt`（经
  `CangJieDecompiledFileViewProvider`，vfile 是 `.cjo`）。
- `CjCommonFile.getFileType()` 曾被硬编码为 `CangJieFileType.INSTANCE`（P0 已归正为 `viewProvider.fileType`）。

## 4. 跨仓边界：主仓 ↔ intellij-ide / deveco

- `intellij-ide` 与 `deveco` 都是**独立 Gradle 构建**，**不通过 project 依赖**引用主仓。
- `intellij-ide/modules/ide/base` 用 `compileOnly(libs.cangjie*ForIde)` 引 11 个发布工件
  （`org.cangnova.cangjie:cangjie-frontend-{common,psi,cfir,analysis-api,...}-for-ide`）。
- **版本号固定**：`intellij-ide/gradle/libs.versions.toml` `cangjie = "1.1.1"`。
- 依赖仓库优先本地：`intellij-ide/settings.gradle.kts` 里 `maven { url = uri("../build/repo") }` 排第一 ⇒ 主仓 `publish` 落 `<root>/build/repo`。
- **推论**：在 `common`/`psi`/`cfir` 新增**类**后，IDE 侧要用上必须
  ① 主仓先 publish；② 版本号不变，IDE 侧需 `--refresh-dependencies` 重新解析，
  否则会出现"改了、发布了、仍报找不到类"的假故障。
- 发布模块：`:prepare:ide-plugin-dependencies:cangjie-frontend-*-for-ide`（11 个）+ `:prepare:ide-plugin-dependencies-module:*`。

## 5. 测试落点约定

- **怎么跑（bash 下的可行做法）**：不要在 bash 里直接执行 `gradlew-queue.bat`（.bat 需要 cmd/PowerShell，
  而 PowerShell 工具不返回 stdout）。**直接调它的 java 入口**：
  ```bash
  export PATH="/usr/bin:/bin:$PATH"
  JAVA="C:/Users/lin17/.jdks/corretto-21.0.9/bin/java.exe"
  "$JAVA" -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar \
      --project-dir "D:/code/intellij/cangjie" :psi:test --tests "*XxxTest*"
  ```
  jar 已存在于 `gradle-queue-cli/build/libs/gradle-queue-cli.jar`（缺失时脚本会自动构建）。
- **加 `--force-no-daemon`**：长寿命 Gradle daemon（见过 uptime 4h54m）会
  `Gradle build daemon disappeared unexpectedly` 而失败；换成 `--force-no-daemon` 立刻成功。
- 首次跑某模块测试约 **9-10 分钟**（要编译依赖链），之后增量 **1-3 分钟**。
- 测试细节（含用例里的 `println`）在 `<module>/build/test-results/test/TEST-*.xml` ——
  探针式调试直接读 XML，比翻控制台快。
- **psi**：`psi/test/org/cangnova/cangjie/psi/`（包名是 `psi`，**不是** `parsing`）。
  先例：`DoubleColonParsingTest.kt`、`ModifierParsingTest.kt`、`DeclarationFileParsingTest.kt`。
  依赖 `testFixtures(project(":tests:test-infrastructure"))`。
  **`.cj.d` 用例怎么写**：`CjParsingTestCase(fileExt = CangJieDeclarationFileType.DECLARATION_EXTENSION,
  fileType = CangJieDeclarationFileType, CangJieParserDefinition())` ——
  它把 `fileType` 直接塞进 `LightVirtualFile`，于是 `viewProvider.fileType` 就是声明类型，
  整个 `createFile` → `CjFile.sourceKind` 链路会走真实路径（无需注册 FileType）。
  一个测试类只能有一种 `fileType`，双向验证要拆两个类。
- **common**：`common/test/org/cangnova/cangjie/...`，junit-jupiter + `kotlin("test")`。
- **CFIR 两条路径的 testFixtures 成对**：`light-tree2cfir` 里 `testFixturesApi(testFixtures(project(":cfir:raw-cfir:psi2cfir")))`
  ⇒ 新增基类通常要两边各一份；护栏类断言（PSI vs LightTree）必须成对写。
  既有双变体先例：`AbstractRawCfirBuilderLazyBodiesByAstTest` / `...ByStubTest`
  ⇒ 走 AST / Stub 会分叉的判定必须两种变体都覆盖。
  变体由 `TestGeneratorForPsi2Cfir.kt` 生成（`generateTestGeneratorTests` 挂在 compileTestKotlin 依赖上），**不要手写**。
- **cfir-serialization**：`cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/{cjo,cjd}/`，junit-jupiter。
  端到端先例：`CjoSdkDeserializationIntegrationTest.kt`（真实 `.cjo` fixture 集成测试）。
- **analysis**：用 `testFixtures(project(":analysis:analysis-test-framework"))`。

## 6. 设计文档

- **`.cj.d` 声明文件支持**，三稿 + 一份复核并存，**以 v3（v3.1 修订版）为准**：
  - `docs/cjd-declaration-file-support.md`（v1，2069 行，有 5 处事实性错误）
  - `docs/cjd-declaration-file-support-v2.md`（v2，2838 行，含已被 v3 推翻的早期结论）
  - **`docs/cjd-declaration-file-support-v3.md`（v3.1 定稿，4299 行，框架正确版 + 已过复核 + 含 P1 实施记录 4.2.8）**
    - 第 8 章「框架正确性评估与破坏性改动索引」是设计依据（判据 J1–J5、破坏性改动 F1–F7、建议不改清单）
    - 附录 C 是版本变更记录（v1→v2→v3→v3.1 的结论更正，**读旧稿前先看它**）
    - **附录 C.5 是复核报告的逐条处置**（含 3 处"未照复核建议做"的理由 + 8 项复核外新发现）
    - 核心取向：**框架正确性优先于改动最小化**，允许必要的破坏性改动
  - **`docs/cjd-declaration-file-support-v3-review.md`（复核报告，541 行）**：R1–R14 与 13 处官方取证全部成立
    （含 DIFF-1 越界 UB 确认）；4 处必修 / 4 处补正 / 4 处澄清。头部已回填"处置状态"。
    **读 v3 前先读这份复核。**
  - 阶段划分 P0–P6 见 5.1；P0＝公共契约（`CjSourceKind` / `CjSourceKindCarrier` / `CjSourceFile.sourceKind` /
    `CjFile.sourceKind` + `getFileType()` 归正 / `compileCjd` / `-d`）。
- 既有架构文档：`docs/cjfir-compiler-stages.md`、`docs/module-catalog.md`、`docs/project-architecture-diagram.md`。
- `docs/` 顶层放文档，`docs/cangjie-tutorial/` 是 VitePress 站点（含 node_modules，不要往里加东西）。
