# CJMP G3/G4：CangjieSemanticsAuthority 语义证据

日期：2026-10-27。范围：G3 文件真实归属、G4 普通源码依赖与 common-part/refinement 依赖中的扩展。

官方仓库：D:/code/intellij/cangjie/external/cangjie_compiler。
所有官方源码内容均通过 `git show v1.1.3:path` 读取，版本固定为 `v1.1.3@14276740ffec1262b81b6b5236b1d840af270b37`。

指定编译器：C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe。
实测版本：`Cangjie Compiler: 1.1.3 (cjnative)`，目标 `x86_64-w64-mingw32`。
官方源码摘录保存在 [版本固定的源码证据](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:1)；每段包含完整原文件路径、git show 命令和原始行号。

## G3：文件与声明的来源不会由查询方决定

原计划 [G3](D:/code/intellij/cangjie/docs/cjmp-framework-review-and-fix-plan-20260928.md:1157) 与 [P7 的 G3](D:/code/intellij/cangjie/docs/cjmp-framework-review-and-fix-plan-20260928.md:2076) 要求：按 content scope 找文件所属模块；use-site 仅在多个实际候选模块之间消歧。

| 官方证据 | 原始位置 | 能证明的事实 |
|---|---|---|
| [编译包与文件绑定](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:7) | CompileStrategy.cpp:167–197、295–297 | 包名来自源码 package 声明；解析后的文件绑定该包；已有文件的 curPackage 一致性是显式不变量。 |
| [声明与原文件绑定](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:47) | Parser.cpp:185–194 | AssignCurFile 将原文件赋给文件中各节点。 |
| [声明包标识保持](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:62) | Collector.cpp:652–655 | 只有 fullPackageName 为空时才赋当前包名；已导入声明已有原包名，不覆盖。 |
| [反序列化来源及 common 标记](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:71) | ASTLoader.cpp:298–324、389–398、591–606 | 导入声明 fullPackageName、moduleName 来自序列化包；common 文件保留 FROM_COMMON_PART、COMMON、isCommon、原路径和 fileHash。 |

必须区分官方编译包中的 AST 合并与平台原始 PSI 归属。ASTLoader.cpp:307 确实执行：

```cpp
curPackage = &pkg; // Deserialize common part AST into current specific package AST
```

这是创建并装入 specific 编译包的 common AST 节点；CreateFileNode 仍恢复原文件路径，文件仍标记为 common。不能把这一步解释为平台应执行 `getModule(commonPsi, specificUseSite) == specificModule`。cjc 没有 CaModule/getModule 接口；content scope 契约来自原计划，官方证据支持保留来源身份。

G3 回归应使用真实、互不重叠的 common/specific 文件目录。查询 common 文件中的原始声明时，use-site 为 specific 或 common 都应返回 common owner；查询普通依赖中的原始声明也应返回依赖模块 owner。common 与 specific 可以有相同的仓颉 package 名，平台模块归属仍由物理文件及其 content scope 决定。声明查找时选中配对的 specific 实现与原始 common PSI 所属模块是两个不同操作。

## G4：扩展收集必须覆盖两类依赖，访问条件继续按真实语义判定

原计划 [G4](D:/code/intellij/cangjie/docs/cjmp-framework-review-and-fix-plan-20260928.md:1178) 与 [P7 的 G4](D:/code/intellij/cangjie/docs/cjmp-framework-review-and-fix-plan-20260928.md:2077) 给出的生产依赖集合为：

```text
dependencies + allRefinementDependencies
```

| 官方证据 | 原始位置 | 语义 |
|---|---|---|
| [源码与导入包的扩展收集](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:147) | TypeCheckExtend.cpp:398–432 | BuildExtendMap 收集当前源码的扩展；BuildImportedExtendMap 收集导入包的扩展，再更新类型扩展索引。 |
| [common-part 的 import](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:131) | ImportManager.cpp:540–550 | 先加载 common-part 文件，再处理包头与 import，以处理 common-part 自己的依赖。 |
| [common-part 加载](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:661) | CjoManager.cpp:311–324、478–487 | 单独装入 common-part；校验 common-part 的 package 名与 specific 包一致。 |
| [生产成员查找](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:491) | LookUpImpl.cpp:151–170 | 对索引中的扩展逐个调用 IsExtendAccessible，随后收集成员。 |
| [官方可见扩展成员入口](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:516) | CompilerInstance.cpp:1045–1095 | GetAllVisibleExtendMembers 使用扩展访问、成员访问与泛型约束过滤。 |
| [扩展及成员的导入/导出条件](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:187) | ImportManager.cpp:1392–1462、1485–1544 | 跨包成员使用需同时满足扩展导出、扩展导入、成员导出、成员导入。 |
| [private/public import 的作用域](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:347) | ImportManager.cpp:1088–1103、1152–1166 | 非重导出 import 存入当前 fileHash 的表；重导出存入包级表。名字查询合并当前文件表和包级表。 |

本版本实际对应的官方生产入口是 GetAllVisibleExtendMembers、FieldLookupExtend、IsExtendAccessible 和 IsExtendMemberAccessible；本报告使用这些已读的版本固定实现核证 GetVisibleExtends 所需的语义。

收集普通依赖的 provider 与成员是否可访问分别由这两层处理。private import 仍是当前文件的合法普通依赖导入，不能据此丢弃整个依赖的扩展 provider。public import 的重导出作用也不会把被依赖库的 private 成员变成可访问成员。

### 合法的导出与有界扩展边界

[导出规则](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:384) 的原始实现位于 Node.cpp:968–981、1180–1249、1270–1285；[导入规则](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:187) 位于 ImportManager.cpp:1392–1462、1485–1544。

- extend 声明本身不能使用 public/private 修饰。[官方解析器检查](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:720) 的 ParseDecl.cpp:1745–1750 对 CJMP 修饰符单独处理，其余非空修饰符调用 DiagExpectNoModifier；ParserModifierRules.cpp:413–414 也明确 extend 的普通修饰符集合为空。应在成员上写 public/private；本次探针两种写法均位于合法的 extend 体内。
- 类型与直接扩展在同一 package 时，公开类型的公开扩展成员可以跨包使用。泛型直接扩展的类型实参及所有上界还必须满足导出规则。
- 在别的 package 中给外部类型添加的直接扩展不导出，即使扩展成员写 public。provider 中给 Int64 添加的 foreignValue 就是这个边界；localForeignValue 在 owner 包中使用它合法。
- 对外部类型的接口扩展，至少一个所实现接口须可导出，所有泛型上界须可导出。包外只导出接口实现成员；额外 public 方法不能自动导出。访问具体成员时须导入该成员所属接口，不是仅导入任意一个其他接口就够。类型同包的接口扩展走随类型导出的分支。
- 对本次同包泛型直接扩展 `extend<T> ProbeBox<T> where T <: Bound`，消费者还须导入 Bound；Fits 实现 Bound 不等于消费者已经导入 Bound。
- 满足导出与导入后，接收者的类型实参仍须满足约束。[成员候选约束过滤](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:572) 位于 TypeCheckReference.cpp:402–432；[泛型扩展实例化筛选](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:608) 位于 InstantiatedChecker.cpp:125–142。

跨不同根包的正例中，类型、构造器、所需上界和扩展成员均显式 public。internal 默认成员只对本包及子包开放；protected 受模块关系限制；private 扩展成员只在本扩展体内可用。[官方包关系过滤](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:631) 位于 ModulesUtils.h:71–95。

docs MCP 的“访问规则”“包的导入”“顶层声明的可见性”“直接扩展”也确认以上语言规则；文档用于补充，版本判定以本次 v1.1.3 源码及指定 cjc 为准。

## 官方 cjc 两包库实测

provider 是 `package g4provider`，各消费者均是独立编译的 `package g4consumer`。先构建 provider 的 staticlib/.cjo，再以 --import-path 编译每个消费者库。两个根包之间不存在 internal 的子包可见性。

[库源码](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/provider/Declarations.cj:1) 中 Receiver 和直接扩展同包；visibleValue 为 public，并在本扩展内调用合法的 privateValue。ProbeBox 的泛型直接扩展使用公开 Bound 上界，Fits 实现 Bound，Misses 不实现。

| 探针 | cjc 退出码 | 精确结果 |
|---|---:|---|
| [普通依赖，private import](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/ordinary-private/Use.cj:1) | 0 | 当前文件可调用 Receiver().visibleValue()。 |
| [普通依赖，public import](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/ordinary-public/Use.cj:1) | 0 | 当前文件可调用相同公开扩展成员。 |
| [跨包调用 private 成员](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/private-member/Use.cj:1) | 1 | sema_not_member_of；privateValue；JSON 主范围 6:16 → 6:28。 |
| [导入上界且满足约束](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/bounded-positive/Use.cj:1) | 0 | ProbeBox<Fits>().boundedValue() 合法。 |
| [漏导入上界](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/bounded-no-upperbound-import/Use.cj:1) | 1 | sema_not_member_of；主范围 6:22 → 6:34；note 精确为“to use the following extension, import 'Interface-Bound'”。 |
| [实参不满足约束](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/bounded-unsatisfied/Use.cj:1) | 1 | sema_not_member_of；ProbeBox<Class-Misses>；主范围 6:24 → 6:36。另有一个 unused import 警告。 |
| [外部类型的直接扩展不导出](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/foreign-direct-negative/Use.cj:1) | 1 | sema_undeclared_identifier；foreignValue；主范围 7:11 → 7:23。 |
| [common-part 库](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/common/Shared.cj:1) | 0 | 输出 g4consumer.cjo/.chir；共享源码自己也调用普通依赖中的公开扩展。 |
| [specific 同时使用两类依赖](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/specific/Use.cj:1) | 0 | 同一函数可调用 Receiver().visibleValue()、CommonReceiver().commonValue() 和 commonCallsRegular()。 |

provider 的最终库构建退出码为 0。总计 6 次成功构建（含 provider），4 个目标负例均失败于预期语义。完整退出码记录在 [results.json](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/results.json:1)，四个失败的官方原始 JSON 分别保存在 [private-member.json](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/diagnostics/private-member.json:1)、[bounded-no-upperbound-import.json](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/diagnostics/bounded-no-upperbound-import.json:1)、[bounded-unsatisfied.json](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/diagnostics/bounded-unsatisfied.json:1)、[foreign-direct-negative.json](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/diagnostics/foreign-direct-negative.json:1)。

这些诊断定义名是官方两包 CJO 模式实测结果。本次没有读取本仓库的诊断映射，也没有执行源码 LL 分析测试。

### 复现命令（PowerShell 7）

所有命令在 D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency 执行。

```powershell
$cjc113 = 'C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe'
$env:CANGJIE_HOME = 'C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie'

& $cjc113 -p provider --output-type=staticlib --output-dir artifacts/provider --diagnostic-format=json
& $cjc113 -p ordinary-private --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p ordinary-public --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p private-member --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p bounded-positive --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p bounded-no-upperbound-import --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p bounded-unsatisfied --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json
& $cjc113 -p foreign-direct-negative --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/consumer --diagnostic-format=json

& $cjc113 --experimental -p common --output-type=chir --import-path artifacts/provider --output-dir artifacts/common --diagnostic-format=json
& $cjc113 --experimental -p specific artifacts/common/g4consumer.chir --common-part-cjo=artifacts/common/g4consumer.cjo --output-type=staticlib --import-path artifacts/provider --output-dir artifacts/specific --diagnostic-format=json
```

common 与 specific 源码的仓颉 package 均为 g4consumer，物理目录不同。共同源码里的 CommonReceiver/extend 是共享实现。--output-type=chir 和 --common-part-cjo 的版本固定定义见 [CLI 定义](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:704)，specific 模式识别见 [CJMP 模式定义](D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/review-20261027/g4-source-dependency/official-v1.1.3-evidence.txt:691)。这里核实了一条 common-part 边；v1.1.3 的 common-part 参数是单次参数，本探针没有引入多 common 父节点的额外语义。

## 给测试作者的回归形状

| 回归 | 模块装配 | 需断言的真实行为 |
|---|---|---|
| G3 普通依赖 owner | provider/consumer 为两个真实源码模块，目录互不重叠 | getModule(provider 文件原始 PSI, consumer use-site) 返回 provider。 |
| G3 common owner | common/specific 为两个真实源码模块，specific 具有 common refinement 边 | getModule(common 文件原始 PSI, specific use-site) 返回 common；同 package 名不改变归属。 |
| G4 普通源码依赖 | consumer.dependencies 包含 provider；无 refinement；使用 provider 的 .cj 源码 | private/public import 两个正例均解析到 provider 文件的 visibleValue 声明。 |
| G4 refinement | specific 具有 common refinement 边；使用 Shared.cj 源码 | CommonReceiver().commonValue() 解析到共享源码中的扩展声明。 |
| G4 两类依赖并存 | specific 普通依赖 provider，同时 refinement common | 两个扩展成员都可解析，各自保留来源声明。 |
| G4 有界/可见性负例 | 基于同一已验证库源码，改变调用或 import | privateValue 不可调用；缺 Bound 或实参不满足 Bound 时 boundedValue 不可调用；外包直接扩展不会被 public 成员修饰符导出。 |

复现 G4 原计划缺口时，应把官方验证过的 .cj 放进源码 dependency 模块，让相关声明来自源码 provider；仅使用 .cjo 验证覆盖的是反序列化 provider，不能证明常规源码 dependency 集合已经接入。优先断言调用目标及声明来源，不以 provider 数量替代语义结果。

本次只写上述限定探针目录，未改生产源码，未运行 Gradle，未派生子代理。
