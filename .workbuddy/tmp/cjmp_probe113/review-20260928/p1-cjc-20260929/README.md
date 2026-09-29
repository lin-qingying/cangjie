# P1 官方 cjc 1.1.3 精确探针（2026-09-29）

已使用本机 cjc 1.1.3 实际运行共 12 组。源码通过 apply_patch 创建。
各组 common/specific 编译均使用独立输出目录，common CJO 未被 specific 输出覆盖。
本次没有运行仓库 Gradle 测试。成功用例进行了完整 dylib 编译/链接，但没有运行生成的 DLL。

版本实测：

```text
Cangjie Compiler: 1.1.3 (cjnative)
Target: x86_64-w64-mingw32
```

完整环境、每一条工作目录/命令、退出码、原始诊断 stdout/stderr 合并输出保存在
`run-results.json`。失败阶段的原始 JSON 另存为每组的
`common.diagnostics.json` / `specific.diagnostics.json`；成功阶段日志为空字符串。
记录时间：2026-09-28 23:30:12 UTC（本地 2026-09-29）。

## 实测结果

| 用例 | common exit | specific exit | common 诊断 | specific 诊断 |
|---|---|---|---|---|
| use3 | 0 | 0 | 0 | 0 |
| use4 | 0 | 0 | 0 | 0 |
| both_implicit_good | 0 | 0 | 0 | 0 |
| both_implicit_bad | 0 | 1 | 0 | sema_return_type_incompatible (specific.cj:3:1) |
| common_implicit_specific_explicit_bad | 0 | 1 | 0 | sema_not_matched (specific.cj:3:1) |
| both_empty_unit | 0 | 0 | 0 | 0 |
| common_empty_specific_implicit_value | 0 | 1 | 0 | sema_return_type_incompatible (specific.cj:3:1) |
| common_value_specific_empty | 0 | 1 | 0 | sema_not_matched (specific.cj:3:1) |
| specific_error_type | 0 | 1 | 0 | sema_undeclared_type_name (specific.cj:3:31)<br>sema_not_matched (specific.cj:3:1)<br>sema_not_matched (common.cj:3:1) |
| common_bodyless_untyped | 1 | 未执行 | parse_common_function_must_have_return_type (common.cj:3:15) | 未执行 |
| specific_bodyless_untyped | 0 | 1 | 0 | parse_specific_function_must_have_return_type (specific.cj:3:17)<br>parse_missing_body (specific.cj:3:29) |
| specific_implicit_error | 0 | 1 | 0 | sema_undeclared_identifier (specific.cj:3:32)<br>sema_return_type_incompatible (specific.cj:3:1) |

## 用例与语义边界

1. `use3`：与 p1-tests/cjmpDefaultReadThroughBeforeImplicitTypes.cj 中的 a/use3 源码一致，补独立 typeWitness():Int64 验证 use3 的推断结果。两段均 0 诊断，证明无显式返回的调用方仍读到 common 默认实参并能调用 specific。
2. `use4`：与 p1-tests/cjmpSpecificShadowingBeforeImplicitTypes.cj 中 platform/use4 源码一致，补独立 typeWitness():Int64。两段均 0 诊断，证明函数体推断不因 common/specific 同签名候选而歧义。
3. `both_implicit_good`：common 和 specific 均省略返回，函数体分别为 1/2，编译成功；typeWitness 验证调用仍为 Int64。
4. `both_implicit_bad`：common 省略返回但已在 CJO 中推断 Int64；specific 返回 String，最初 Quest 可配对，后检查仅报 RETURN_TYPE_INCOMPATIBLE。
5. `common_implicit_specific_explicit_bad`：specific 显式 String，预配对就已不兼容，仅报 specific NOT_MATCHED；common 有默认体，故无 common NOT_MATCHED。
6. `both_empty_unit`：双方空函数体，无显式返回，预检为 Unit；0 诊断，typeWitness 为 Unit。
7. `common_empty_specific_implicit_value`：common Unit，specific 有非空体而初始 Quest，先配对再报 RETURN_TYPE_INCOMPATIBLE。
8. `common_value_specific_empty`：common CJO 中 Int64，specific 空体预检 Unit，直接 NOT_MATCHED。**无显式返回不等于真实 Quest；空体必须区分。**
9. `specific_error_type`：specific 显式 Missing 返回，先报 UNDECLARED_TYPE_NAME，再两侧 NOT_MATCHED；不能把错误类型当 Quest 接受。
10. `common_bodyless_untyped`：没有函数体的 common 必须声明返回；common 编译失败，不执行 specific。
11. `specific_bodyless_untyped`：没有函数体且无返回类型的 specific 同时报必须声明返回和缺失 body。
12. `specific_implicit_error`：specific 初始 Quest，函数体后续出现 absent 未声明错误；保留配对并在后检查报 RETURN_TYPE_INCOMPATIBLE，**没有 NOT_MATCHED**。不能把后续错误反写成“未配对”。

这些两段式探针里 common 已经编译成 CJO，所以验证的是 common 省略类型的合法性及最终返回关系；
不能用它们宣称实测了 LL 中 common 与 specific 同时仍为 Quest 的锁内状态。任侧 Quest 接受、
checker 后置复核的 LL 方案以 VERSION-EVIDENCE-20260929.md §6 的官方 PreCheck/TypeManager/CheckCJMP 源码为依据。

## 命令形状

在每组目录执行（实际包名和每组完整参数见 run-results.json）：

```powershell
$env:CANGJIE_HOME='C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie'
$env:Path="$env:CANGJIE_HOME/bin;$env:Path"
& "$env:CANGJIE_HOME/bin/cjc.exe" common.cj --experimental --output-type=chir --output-dir common_out --diagnostic-format=json
& "$env:CANGJIE_HOME/bin/cjc.exe" specific.cj common_out/<package>.chir --experimental --common-part-cjo=common_out/<package>.cjo --output-type=dylib --output-dir specific_out -o probe.dll --diagnostic-format=json
```

