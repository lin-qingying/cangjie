# CJMP 实施续接（2026-10-27）

## 工作位置和完整范围

- 原会话：`切换到 cjmp-framework-implementation`。
- 实施工作树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支：`codex/cjmp-framework-implementation`；本次开始时 HEAD 为 `f3faaa163`。
- 根仓库 `D:/code/intellij/cangjie` 的 main 工作区另有未解决冲突，本批代码在上述实施工作树修改。
- 完整目标仍为 `cjmp-framework-review-and-fix-plan-20260928.md` 的 P0–P10 与原 70 项；H6 的 CHIR/后端剩余部分按原用户要求排除，不计作完成。
- 历史完成度不能证明当前要求已验收；以源码、官方证据与本轮实际测试为准。

## E6 本批生产修改

1. `common/.../CjmpDeclarationDefaults.kt` 提供共享 `hasCjmpDefaultImplementation`，严格先检查 specific，再读取实现事实。
   parser 投影真实直接成员的函数体、getter/setter 与初始化器；CFIR 的 `cjmpHasCommonDefault` 使用同一判据，并保留 CJO 原始 COMMON_WITH_DEFAULT 位。
2. `AbstractCangJieParsing.consumeUntilAny` 与成对括号恢复对位官方 `ParserUtils.cpp`。
   属性非法前缀按调用文法要求恢复到物理换行或分号，不自行增加右花括号停止条件。
   参数列表丢弃非法后缀后返回本列表的结束符或正文边界；getter/setter 的参数边界不把 `=` 当作正文起点。
3. `MacroSurfaceToken.isSourceToken` 显式区分原源码坐标与生成文本坐标。
   PSI/LightTree 捕获、重新分词、子宏范围替换、`MacroFragmentSourceMap` 与 executor 参数位置投影均消费该标记；不能根据数值范围重叠推断来源。
4. source map 支持 EOF 零宽位置；跨来源 token 无法形成同一逐字符区间时，不生成伪造的精确区间。
   稳定点比较包含来源坐标，达到迭代上限后未稳定必须失败。`MacroFragmentParser.VERSION` 已升至 25。
5. 生产宏回归使用真实 PSI 宿主范围验证两种重解析入口的生成语法位置。
   父宏参数回归同时核验源码 token 与两个子宏结果的协议位置；provider 回归覆盖重叠数值、EOF 与非收敛复扫。

## 官方证据

官方镜像 checkout 为 v1.0.0，引用必须用 `git show v1.1.3:<path>`。
本批核对了 `ParseCJMPDecl.cpp` 的 HasDefault/CheckSpecificInterface、`ParserUtils.cpp` 的恢复程序、
`ParseDecl.cpp` 的 ParseFuncParameters/ParseParameterList/ParsePropMemberBody，以及宏的 originPos/IsCurFile 处理。
Kotlin 参考使用 `compiler/psi/parser/.../parsing`、FIR light-tree parsing listener 与 compiler messages 框架。

实际编译器为：

```text
C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe
Cangjie Compiler: 1.1.3 (cjnative)
Target: x86_64-w64-mingw32
```

实测源文件、原始 JSON 诊断和 AST 保存在：

- `.workbuddy/tmp/cjmp_probe113/review-20261027/e6-parser-range/`：非法属性前缀、嵌套括号、分号边界和 getter 类型后缀。
  同行恢复会跨过右花括号；getter 类型后缀的官方 AST 仍保留真实参数、正文和后续顶层函数。
- `.workbuddy/tmp/cjmp_probe113/review-20261027/e8-outer-constraints/`：外层 T→U、方法 V→W 的等价约束两阶段编译 exit 0；增加 Extra<U> 后仅报 `sema_generic_constraint_not_looser`。
- `.workbuddy/tmp/cjmp_probe113/review-20261027/e9-private-signatures/`：不同参数类型的 private 重载 exit 0；common-private 在先只报 common-private，普通 private 在先则只报 duplicate。
  官方 common-private 分支不插入已见签名集合。这些 E8/E9 探针是取证，不代表仓库实现验收。

## 已核实的测试

下表仅属于本轮最后一次目标命令；XML 时间为 UTC。

| 范围 | 数量 | 失败/错误/跳过 | XML 时间 |
| --- | ---: | --- | --- |
| CjmpDeclarationRulesParsingTest 与 CjmpFunctionParsingTest | 43 | 0 / 0 / 0 | 2026-10-26 22:00:06–08 |
| cfir:providers 完整模块 | 78 | 0 / 0 / 0 | 2026-10-26 22:00:20–21 |
| FrontendMacroConstructionExecutionTest 完整类 | 31 | 0 / 0 / 0 | 2026-10-26 22:00:42 |

该命令实际退出码 0：

```powershell
java -jar 'D:/code/intellij/cangjie/gradle-queue-cli/build/libs/gradle-queue-cli.jar' --project-dir 'C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie' --timeout-minutes 30 :psi:test --tests '*CjmpDeclarationRulesParsingTest' --tests '*CjmpFunctionParsingTest' :cfir:providers:test :compiler:frontend:test --tests '*FrontendMacroConstructionExecutionTest' --no-build-cache --continue --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8'
```

先前的完整 PSI 命令执行 382 项，最后仅剩 accessor 诊断测试筛选器漏收重复诊断；本批已修正并在上述目标范围验证。
不能将这条先前失败的完整命令记成全量通过。完整 CFIR、LL、PSI 与两阶段编译 gate 仍须取得新结果。

完整 gate 的最新已完成子任务：`:psi:test` 为 382/382，0 failure/error/skipped，XML UTC 时间为 2026-10-26 22:22:50–55。
同一命令的 CFIR 测试仍在运行，已经观察到 `staticCannotBeOpenAbstractOverride`、`privateExtendOwnerGroups`、
declaration-status coverage 及若干 LLT 用例失败；尚未取得该轮完整结果，未判定它们是否由本批修改引入。
不要把这些失败删除或把完整 gate 记作通过。验证结束后应先导出全部失败及实际/预期差异，再分类处理。

## 继续工作顺序

1. 核对本聊天最后一次 Gradle handle 及队列 owner 的真实进程状态。终态结果才允许新启动；不能仅凭 journal 或旧 XML 判断仍在运行。
2. 完成当前工作树完整 `:psi:test :cfir:analysis-tests:test :analysis:low-level-api-cfir:test`，以及 `CjmpTwoPhaseCompilationTest` 和 `validateDocumentation`。
   按实际失败分类修共享路径，不删期望掩盖问题；E6 的整体验收和 P0–P10 总体验收尚未确认。
   本聊天已启动该命令，当前 unified exec session 为 `59277`；使用 `write_stdin` 接续观察，终态后再读取各模块 XML。
3. E8 当前 `CfirCjmpMatchingContext.incompatibleGenericConstraintIndices` 已消费 counterpart 的完整映射；应补外层约束回归并核对所有生产/LL/CJO 路径，不能照历史缺口重复发明实现。
4. E9 当前 checker 仍使用目标类型文本和按成员名分组。按上述官方有序判据替换，共用框架签名/类型表示，保留 common-private 不插入集合及 else-if 的单诊断顺序。
5. 继续逐项审计 C3、C4、E7–E9、E12、F4、G1/G3/G4/G5，以及 P0 历史版本与 P10 总体验收。
   MppCheckerKind、collector 分类与 LL 两类入口当前已经存在；历史“缺少整个抽象”的描述不能作为现状。
6. 验收完成后只暂存本批相关文件或 hunk；工作树中已有大量其他源码及生成文件变更，不做批量清理、回退或提交。

前置子代理与后续独立复审调用均因模型服务限流失败，主线程已直接完成源码及官方语义读取；未取得独立 ACCEPT。
本记录不标记完整目标完成，也不重算整体完成比例。
