# LL CJMP 新增 fixture 的官方验证（2026-09-30）

版本：`Cangjie Compiler: 1.1.3 (cjnative)`，目标 `x86_64-w64-mingw32`。
实际运行于 2026-09-30 15:23:25 UTC（北京时间 23:23:25）。

源码取自工作树
`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
仅移除多模块测试基建指令并按 FILE 拆分；仓颉声明、函数体、类型和调用未改写。
探针源、本记录与 JSON 运行记录均使用 apply_patch 保存。
每组独立 common_out/specific_out，specific 只输入该组的一份 common CJO/CHIR。

## 实测结果

| 源 fixture | common exit | specific exit | 诊断 |
|---|---|---|---|
| diagnostics/cjmpMemberCallerFirst/extendMemberDefaultCallerFirst.cj | 0 | 0 | 两段均 0 error、0 warning，原始日志为空 |
| diagnostics/cjmpMemberCallerFirst/extendMemberShadowedCommonCallerFirst.cj | 0 | 0 | 两段均 0 error、0 warning，原始日志为空 |
| diagnostics/cjmpCallerFirst/uncalledImplementationCallerFirst.cj | 0 | 0 | 两段均 0 error、0 warning，原始日志为空 |

三个 specific 编译都成功生成 dylib；没有运行 DLL，不声称测量了运行值。
精确编译器路径、版本输出、工作目录、参数、退出码、原始 stdout/stderr 合并内容见
[run-results.json](run-results.json)。

## 对测试期望的结论

- 普通 `public class Item {}` 可以作为 `common extend Item` 的目标，
  并由 `specific extend Item` 提供实现；无需给 Item 本身加 common/specific。
- common extend 成员的命名参数默认值 `x!: Int64 = 1` 可以在 specific 实现省略默认值；
  caller 的 `item.value()` 在两段式官方编译中成功。
- common/specific 同签名 extend 成员不会使 `item.value()` 报歧义。
- used()/unused() 各自都有 specific 实现时，caller 只调用 used() 仍然零诊断；
  unused 实现在独立 uncalled.cj 文件中，不应因“尚无调用”产生 common NOT_MATCHED。
- 这些探针确认语言语义；LL 按需请求的相位、锁及不预热 unused body 的要求，由本仓新增回归测试验证。
  本次没有运行 Gradle，没有修改本轮编译使用的测试或生产文件。

## 官方静态依据（git show v1.1.3，非 external 当前 checkout）

- `src/Sema/CJMP/CheckCJMP.cpp:387–455 MergeCJMPExtensions`：
  用 extend 的目标类型、继承接口、泛型约束配对并合并；不要求被扩展 nominal 带 COMMON。
- `:948–964 MatchCJMPFunction`：
  common 参数有 desugarDecl 而 specific 没有时，复制默认参数 assignment/desugarDecl 并置 HAS_INITIAL。
- `:1193–1214 MatchCJMPDecls`：
  先遍历所有 specific 声明建立对应关系，再扫描 common 报未匹配，独立于函数是否被调用。
- `src/Sema/TypeCheckCall.cpp:2300–2301`：
  调用候选在配对后经 RemoveCommonCandidatesIfHasSpecific 去掉已实现的 common 候选。

## 每组命令

各组 cwd 为同名子目录，环境 CANGJIE_HOME 为
`C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie`；
其 bin 目录加入当前 PowerShell 进程 PATH。

### extendMemberDefaultCallerFirst

```text
cjc common.cj --experimental --output-type=chir --output-dir common_out --diagnostic-format=json
# exit=0; output=""
cjc caller.cj callee.cj common_out/cjmp_ll_member_default_first.chir --experimental --common-part-cjo=common_out/cjmp_ll_member_default_first.cjo --output-type=dylib --output-dir specific_out -o probe.dll --diagnostic-format=json
# exit=0; output=""
```

### extendMemberShadowedCommonCallerFirst

```text
cjc common.cj --experimental --output-type=chir --output-dir common_out --diagnostic-format=json
# exit=0; output=""
cjc caller.cj callee.cj common_out/cjmp_ll_member_shadow_first.chir --experimental --common-part-cjo=common_out/cjmp_ll_member_shadow_first.cjo --output-type=dylib --output-dir specific_out -o probe.dll --diagnostic-format=json
# exit=0; output=""
```

### uncalledImplementationCallerFirst

```text
cjc common.cj --experimental --output-type=chir --output-dir common_out --diagnostic-format=json
# exit=0; output=""
cjc caller.cj callee.cj uncalled.cj common_out/cjmp_ll_uncalled_first.chir --experimental --common-part-cjo=common_out/cjmp_ll_uncalled_first.cjo --output-type=dylib --output-dir specific_out -o probe.dll --diagnostic-format=json
# exit=0; output=""
```

