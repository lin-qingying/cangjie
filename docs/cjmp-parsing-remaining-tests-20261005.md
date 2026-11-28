# 剩余五项 CJMP parsing 规则：官方测试证据（2026-10-05）

本次只创建独立官方编译器探针，没有修改现有 parser、checker、fixture，没有运行仓库 Gradle。
依据为 `cjmp-parsing-remaining-rules-evidence-20261005.md` 和固定官方 Git 对象。
源码、可复现脚本及编译器原始 stdout/stderr 位于
`.workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/`。
所有源码通过 apply_patch 创建，输出目录是各自 `<case>-out/`。

## 工具身份与执行边界

实际执行 `C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe --version`：

```text
Cangjie Compiler: 1.1.3 (cjnative)
Target: x86_64-w64-mingw32
```

PATH 中的 cjc 实为 1.0.5，本次没有用它代替 1.1.3。执行命令：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/run.ps1
```

脚本先执行该 SDK 的 envsetup，检查精确版本，再逐个编译 25 份独立源码。
common 用 `--experimental --output-type=chir --diagnostic-format=json`；
specific 同时传入真实生成的 `common-seed-out/remainingparsing.chir` 和
`--common-part-cjo=<绝对路径>/common-seed-out/remainingparsing.cjo`，输出 dylib。
普通文件也输出 dylib。所有调用显式限制 `--output-dir` 到当前探针目录。
原始矩阵 25 份全部执行完成；common-seed、common-ordinary-static-counted、common-secondary exit=0，
其余 exit=1。随后新增三份文件角色探针及一份开头两空行的文件角色探针，均单独执行且 exit=1，
总计 **29 份源码、29 组输出、59 条诊断**。
原始 JSON 中的额外 sema/FFI 错误全部保留，不能把 exit=1 一律解释为目标语法错误。

首次环境脚本受到签名执行策略阻止，改为上述单进程 Bypass 后执行。
specific 命令探索曾漏传 CHIR 或错误使用无等号 CJO 参数，得到 driver invalid-options，
这些结果不作为语义证据。下表只记录修正命令后最后一次完整运行的 stderr.json。
没有运行 DLL，没有使用 alpha.20 编译器。

## 实测矩阵

范围均为官方 `MainHint.Range.Begin/End` 的一基 line:column 原值；不能用 Location 的起点
冒充完整范围。所有源文件均在上述探针根目录，表内省略 `.cj`。

| 用例 | 实测诊断及范围 | 证明的边界 |
| --- | --- | --- |
| common-seed | 无诊断，exit=0 | 真实 common CJO/CHIR 输入可用 |
| interface-missing | parse_specific_member_must_have_implementation：3:5–3:26、4:5–4:32 | 普通无体函数、无访问器 property 要实现；第5行 own specific 无体函数豁免该诊断 |
| interface-bodies | 只有 sema_not_matched：5:5–5:39、2:1–6:2 | 函数体、getter 和 own specific 均满足 HasDefault；不是匹配正确性的正例 |
| interface-parent-short-circuit | parse_cjmp_outdecl_miss_match：3:5–3:33、4:5–4:34 | 两个直接成员分别报告；第一处失败不能停止后续成员检查 |
| common-parent-short-circuit | parse_cjmp_outdecl_miss_match：3:5–3:31、4:5–4:32 | 普通 class 中两个 common 成员均报告，无自行添加的 ctor error |
| common-static-counted | parse_cjmp_static_init：3:5–3:28 | common static init 整声明报错，仍计入父 common class 显式构造器；没有 ctor required |
| specific-static | parse_cjmp_static_init：3:5–3:30 | specific static init 同样禁止 |
| common-ordinary-static-counted | 无诊断，exit=0 | 没有 own CJMP 的 static init 也满足 common ctor presence；不能只统计 instance init |
| common-secondary | 无诊断，exit=0 | common bodyless secondary init 合法并计数 |
| common-no-ctor | parse_cjmp_in_common_ctor_required：2:14–2:26、3:15–3:28、4:23–4:38 | class、struct、abstract class 均报告名字 Identifier；abstract 不豁免 |
| common-primary | parse_illegal_modifier_in_scope：3:5–3:11、3:14–3:20 | v1.1.3 的 common primary ctor / member parameter 有独立 modifier grammar 限制；不是合法纯正例；没有目标 mismatch 或 ctor required |
| common-primary-parameter-mismatch | parse_illegal_modifier_in_scope：3:7–3:13、3:32–3:38；parse_cjmp_outdecl_miss_match：3:7–3:30、3:32–3:56 | 参数与自身普通 primary ctor 比较，不能与 common class 比较；两个参数继续报告，包含完整参数范围 |
| specific-primary-parameter-mismatch | parse_illegal_modifier_in_scope：3:7–3:15、3:34–3:42；parse_cjmp_outdecl_miss_match：3:7–3:32、3:34–3:60 | specific member 参数同样比较自身 ctor；独立 grammar 错误不可删掉 |
| common-member-boundaries | parse_cjmp_outdecl_miss_match：4:5–6:6 | 报告包含 public/common 与整个函数体，不包含上一行 Deprecated 注解；后续 after 函数不在范围内 |
| abstract-ordinary | parse_illegal_modifier_in_scope：3:5–3:13、4:5–4:13；parse_explicitly_abstract_only_for_cjmp_abstract_class：3:5–3:29、4:27–4:28 | 普通 abstract class 不能据此豁免；function 整声明、property 点附近范围明显不同 |
| abstract-common-owner | 只有 sema_invalid_member_visibility_in_class：4:19–4:20、5:19–5:20 | common abstract owner 豁免目标 parsing 限制；未 public 的真实 sema 限制仍存在 |
| abstract-specific-owner | sema_invalid_member_visibility_in_class：4:19–4:20、5:19–5:20；sema_not_matched：2:1–6:2；sema_cjmp_non_specific_abstract_member_in_specific_class：4:5–4:29、5:5–5:27 | specific abstract owner 同样豁免目标 parsing 错误，真实 sema 另管 non-specific abstract member |
| abstract-own-common | parse_cjmp_outdecl_miss_match：3:5–3:36、4:5–4:34；parse_illegal_modifier_in_scope：3:12–3:20、4:12–4:20 | own common 抑制目标 explicitly abstract 报告，但不免父 mismatch 或别的 modifier 规则 |
| abstract-with-bodies | sema_explicitly_abstract_can_not_have_body：4:5–4:42、5:5–5:50 | 合法 CJMP abstract owner 中 function/property 带体是 sema，不应搬成 grammar 或删掉 |
| java-mirror | parse_java_mirror_prop_is_deprecated warning：5:5–5:34；sema_member_not_imported：3:23–3:24；两处 sema_java_mirror_interoplib_must_be_imported：3:1–6:2、3:17–6:2 | `@JavaMirror["probe.C"]` 实际 grammar 赋 builtin 身份并豁免目标 abstract 限制；缺 FFI imports 的附加错误保留 |
| java-impl | parse_java_impl_cannot_be_abstract：3:17–3:18；parse_illegal_modifier_in_scope：4:12–4:20、5:12–5:20 | `@JavaImpl["probe.C"]` 的 abstract owner 自身非法，但未出现目标 explicitly abstract 报告；不能把 broken owner 身份丢掉 |
| java-impl-member | parse_illegal_modifier_in_scope：4:12–4:20、5:12–5:20 | 非 abstract JavaImpl owner 的 abstract 成员依然非法 modifier；目标 CJMP abstract restriction 仍被 subtype provenance 豁免 |
| objc-mirror | parse_explicitly_abstract_only_for_cjmp_abstract_class：4:5–4:36、5:34–5:35 | `@ObjCMirror["ProbeC"]` 不是 JavaMirror abstract 豁免；property 与 function 范围不同 |
| quote-common-not-ctor | parse_cjmp_in_common_ctor_required：2:14–2:15 | quote(common init()) 不能成为宿主实际构造器 |
| macro-common-carrier | 只有 sema_undeclared_identifier：3:6–3:7、4:6–4:7 | 未定义 @Unknown 的真实 carrier 输入只证明宏名称 sema 阶段被阻止；没有实测展开后的 modifier mismatch，不能据此宣称完整 macro parity |
| file-role-common-in-ordinary | parse_common_in_non_common_file：1:1–1:2 | 普通 dylib 编译无 common 模式；实际声明在第5行，报告原文件开头 |
| file-role-specific-in-ordinary | parse_specific_in_non_specific_file：1:1–1:2 | 普通 dylib 编译无 specific 模式；实际声明在第5行，报告原文件开头 |
| file-role-mixed-in-both-modes | parse_common_and_specific_in_the_same_file：1:1–1:2 | 同时开启 common/specific 编译仍拒绝同文件混合；common第5行、specific第6行，报告原文件开头 |
| file-role-common-leading-empty-lines | parse_common_in_non_common_file：1:1–1:2 | 前两行完全空白，仍以原文件1:1–1:2展示 point 范围；End不是2:1 |

## 后续独立 scope / 构造器 owner 探针

后续在同一目录另建 7 个 `authority-*` 用例，各有独立 `*-out` JSON；前述 29 份原始输入与
输出均未覆盖。以下仍使用实际 cjc 1.1.3，而非仓库实现推导测试预期。

- authority-nested-scope-matrix / authority-nested-extend-scope：class、extend 内分别嵌套
  class/interface/struct/enum/extend；interface/struct/enum 内另嵌套 class，均报
  parse_unexpected_declaration_in_scope，主范围仅声明关键字，不是整声明。
- authority-nested-own-ctor：common Outer 仅有 Inner（Inner 有 init），Outer 仍缺构造器，
  名字范围 3:14–3:19；Inner 的 scope 错误范围 4:5–4:10；Following 自己的 init 不受影响。
- authority-macro-only-ctor / authority-macro-parenthesized-only-ctor：common OnlyMacro 仅有
  未定义宏携带 init，均只报 ctor-required，名字 3:14–3:23。carrier 不计为直接构造器已实证。
- authority-function-local-primary：函数体里的 Local 和具名 Local() 被保留为声明形状，
  仅报 unexpected class declaration in function body，class 范围 5:9–5:14。
- authority-macro-token-primary：common Outer 的括号 token 输入含 Input 和 Input()，
  仅 Outer 缺构造器，名字 3:14–3:19。不能把输入中的构造器提升到宿主。

上述范围不能用于将剩余五项 CJMP 整声明诊断缩成关键字；它们证明的是独立 nominal scope
规则及实际构造器归属。当前 parser 的旧 nested nominal 整声明 ERROR 包裹宽于 native。
后续修正仅在 scope keyword 保留错误节点和直接声明结构；五项规则所需的完整声明 ERROR
容器/stub 协议仍须另行实现和验证。

## 原始三种文件角色探针详情

三份源码均有第1行注释、第2行空白、第3行 package、第4行空白。声明不在文件开头，
仍实际得到 `Location=1:1`、`MainHint.Range.Begin=1:1`、`End=1:2`，各自只报告一条 parse error。
Begin/End/Location 的 File 均等于相应原始 `.cj` 探针路径。范围覆盖文件第一个 `/` 字符，
不是 common/specific token，不是 package，不是整个文件，也不是 native JSON 的零宽范围。
这为原文件 0 偏移归属提供官方证据，不证明本仓库当前范围长度或宏展开归属实现正确。

脚本入口分别为（工作目录为固定工作树根目录，路径前缀同上述探针目录）：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/run-file-role-common-in-ordinary.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/run-file-role-specific-in-ordinary.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/run-file-role-mixed-in-both-modes.ps1
```

前两份没有 `--common-part-cjo` 且 `--output-type=dylib`，按官方 SetCompileOptions 两种模式
均关闭。第三份显式 `--output-type=chir` 并传入真实 common CJO 与 CHIR，按相同源码 common
和 specific 均开启；实报 mixed 诊断，没有 driver invalid-options。依赖原 run.ps1 生成的
seed，不改任何此前验证的源码文本，也不覆盖此前25份的输出。

| 原始 JSON（相对于探针目录） | 官方 Message |
| --- | --- |
| file-role-common-in-ordinary-out/stderr.json | common declaration must be defined in common package part |
| file-role-specific-in-ordinary-out/stderr.json | specific declaration must be defined in specific package part |
| file-role-mixed-in-both-modes-out/stderr.json | 'common' and 'specific' declarations can not be in the same file |

上述脚本也保留各自 stdout.json。三种错误均为 DiagCategory=parse、Severity=error，
Num.Errors=Num.PrintedErrors=1，Warnings=0。真实宏展开的未测边界没有因此改变。

### 文件开头空行的 point 展示范围

另行只执行新增入口 `run-file-role-common-leading-empty-lines.ps1`，没有重跑原28份。
`file-role-common-leading-empty-lines.cj` 前两行为空行，UTF-8 文件前四字节实读为
`10,10,112,97`（两个 LF 后紧接 package 的 `pa`，无 BOM）。声明在第5行。
官方 JSON 仍报告 Location=1:1、MainHint.Range.Begin=1:1、End=1:2，
Num.Errors=Num.PrintedErrors=1，Warnings=0，exit=1。
原始输出为 `file-role-common-leading-empty-lines-out/stderr.json` 及 stdout.json。

因此 native 的 point 展示 End 是同一行 column+1；不能把 point 起点 offset 0 的 End
当作真实文本 offset 1 后再经过换行映射，否则本例会错误得到2:1。
空行上的1:2没有对应的普通源字符范围；其 native 展示含义需由独立 point 协议表达。
这项实测不从本仓库的版本7实现推导，也不声称已经验证版本7的修正。

## 完整消息与范围的重用

每份 `<case>-out/stderr.json` 保存 `DiagKind`、`DiagCategory`、`Severity`、`Message`、
`Location`、完整 `MainHint.Range`、OtherHints/Notes/Helps 和计数。`stdout.json` 同时保留。
目标五个官方名称分别是：

- parse_specific_member_must_have_implementation
- parse_cjmp_outdecl_miss_match
- parse_cjmp_static_init
- parse_cjmp_in_common_ctor_required
- parse_explicitly_abstract_only_for_cjmp_abstract_class

诊断定义原文或实测 Message 应直接读取 JSON，不通过本仓库 checker 行为反推。
本次不创建 CFIR inline marker：上述错误应由 parsing 管理，额外 sema 诊断需要分别保留。

## 仅源码证据、尚未完成运行覆盖的部分

官方 `v1.1.3:src/Parse/ParseCJMPDecl.cpp:142–188,242–277` 明确父 direct-member mismatch
令父检查短路，跳过 specific interface / parameter / ctor 后检查；子声明自身有诊断不自动
短路父检查。实测覆盖了多成员继续报告及非法 static ctor 仍计数，但没有构造出同时触发
“own specific interface 与 common member”的纯 mismatch 用例：1.1.3 的文件角色门会先拒绝
common/specific 混合并清除该声明的 modifier。后续 parser 生命周期单测必须区分文件角色门、
子声明检查和父后置检查，不把从源码推导的短路误标为此端到端探针已实测。

尚需真实已定义 declaration macro 展开后触发五项规则，并覆盖 quote 内 nested macro、
宏声明继承真实原父容器、声明头缺 token / 缺闭合 body、不同注解顺序、JavaMirror subtype
带继承、普通函数非 member 参数及 paramLists.size != 1 的实际可达文法。
这些未测部分没有填造诊断范围，不作为整体闭合证据。现有源码有明确 parser 规则，
但 complete runtime/IDE/LightTree/PSI matrix 仍需主线程完成。

## 固定版本源码差异，未做 runtime 猜测

已重新读取 `git diff v1.2.0-alpha.19 v1.2.0-alpha.20 -- src/Parse/ParseCJMPDecl.cpp`：

- alpha.20 common/specific 文件角色门变为处于任一 CJMP 编译模式即可，同文件混合禁止删除。
- common inner 允许 outer COMMON 或 SPECIFIC；specific inner 仍要求 outer SPECIFIC。
- 函数 / primary ctor 的 CheckCJMPFuncParams 调用及实现删除。
- own SPECIFIC 的 HasDefault、static init 和 common ctor presence 在该文件差异中未改变。

`git grep` 固定 v1.0.0 的 parser/diagnostic 来源未发现上述 CJMP 两个名称，且参考取证文档
已确认该版本没有 common/specific token 与 CJMP parser。不能把 1.0.0 / alpha.20 源码差异
说成 1.1.3 编译器的 runtime language-version 开关。本次没有运行 1.0.0 或 alpha.20 二进制。
