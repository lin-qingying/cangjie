# P1 first-fit、占用与缺体顺序取证（2026-09-29）

使用 cjc **1.1.3 (cjnative), x86_64-w64-mingw32** 完成 11 组探针。
每组只生成/输入 **一份 common CJO/CHIR**，common 与 specific 输出目录分离。
没有把 1.1.3 不支持的多个 common CJO 输入当作成功证据。源码与本报告均通过 apply_patch 保存；
未改生产代码、未运行 Gradle。

完整命令、工作目录、退出码和原始日志见 `run-results.json`；
各失败阶段的原始 JSON 另存为 `common.diagnostics.json` 或 `specific.diagnostics.json`。
诊断顺序均为 JSON Diags 数组中实际打印的顺序。

## 1. 已确认的结论

1. 官方源码明确定义：在某个 common 已占用时，TrySetSpecificImpl 报
   MULTIPLE_COMMON_IMPLEMENTATIONS 并返回 false；外层继续尝试后续 common，
   直到某次成功才停止。这是源码已证事实。
2. 本轮尝试的“单 common part 内两个同 key common extend，各含一个同签名 public common
   函数”被 common 编译拒绝，无法合法产出该多候选 CJO：
   - 相同参数：两条 EXTEND_MEMBER_CANNOT_SHADOW；
   - static/instance：静态/非静态同名错误和 shadow 错误；
   - 函数泛型的约束不同：generic_constraint_not_looser 和 shadow 错误；
   - 仅 named 参数名不同：两条 sema_param_named_mismatched；
   - 将 owner 从 Int64 改为 Box<T> 后，上述相同参数和 named 参数结论不变。
3. 因而没有获得“两个完全兼容 common + 两个 specific”的**合法完整程序**实测。
   不将有限探针提升为“不可能构造任何合法程序”的定理，也不伪造失败 common 的 CJO 继续编译。
4. 合法的单 common CJO 加非法的两个同签名 specific，真实输出：
   overload_conflicts → MULTIPLE_COMMON_IMPLEMENTATIONS（锚 common）→ 第二个 specific NOT_MATCHED。
5. 占用判定优先于缺体判定已通过对照实测：
   - common abstract A 中有 default pick，specific 先 concrete pick、后 abstract pick：
     同样是 overload_conflicts → MULTIPLE_COMMON_IMPLEMENTATIONS → NOT_MATCHED，
     **没有** SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION。
   - 同一形状只留下一个 abstract specific pick：仅打印
     SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION。
   - 不根据源码额外猜测实际 JSON 没打印的诊断，也不把非法 specific 程序标为可成功编译。
6. 旧 `firstfit_common.cj` 风格的 named 参数方案不是合法多候选 CJO 的已验证依据。
   本轮等价的 Int64 和 Box<T> owner 方案都在 common 阶段报 named 参数失配。
   旧草案需要标注为待修订，不能直接据其“官方探针形状”描述要求测试转绿。

## 2. 全部实测结果

| 用例 | common exit | specific exit | common 诊断 | specific 诊断 |
|---|---|---|---|---|
| two_common_two_specific | 1 | 未执行 | sema_extend_member_cannot_shadow (common.cj:4:24)<br>sema_extend_member_cannot_shadow (common.cj:7:24) | 未执行 |
| two_common_one_specific | 1 | 未执行 | sema_extend_member_cannot_shadow (common.cj:4:24)<br>sema_extend_member_cannot_shadow (common.cj:7:24) | 未执行 |
| one_common_two_specific | 0 | 1 | 0 | sema_overload_conflicts (specific.cj:5:26)<br>sema_multiple_common_implementations (common.cj:4:5)<br>sema_not_matched (specific.cj:5:5) |
| two_common_static_instance | 1 | 未执行 | sema_static_and_non_static_member_cannot_have_same_name (common.cj:4:5)<br>sema_extend_member_cannot_shadow (common.cj:4:31)<br>sema_static_and_non_static_member_cannot_have_same_name (common.cj:7:5)<br>sema_extend_member_cannot_shadow (common.cj:7:24) | 未执行 |
| generic_bounds_two_two | 1 | 未执行 | sema_generic_constraint_not_looser (common.cj:6:44)<br>sema_extend_member_cannot_shadow (common.cj:6:24)<br>sema_generic_constraint_not_looser (common.cj:9:44)<br>sema_extend_member_cannot_shadow (common.cj:9:24) | 未执行 |
| named_parameter_firstfit | 1 | 未执行 | sema_param_named_mismatched (common.cj:4:24)<br>sema_param_named_mismatched (common.cj:7:24) | 未执行 |
| occupied_before_missing_body | 0 | 1 | 0 | sema_overload_conflicts (specific.cj:5:35)<br>sema_multiple_common_implementations (common.cj:5:5)<br>sema_not_matched (specific.cj:5:5) |
| unoccupied_missing_body | 0 | 1 | 0 | sema_specific_member_must_have_implementation (specific.cj:4:5) |
| generic_owner_two_two | 1 | 未执行 | sema_extend_member_cannot_shadow (common.cj:7:24)<br>sema_extend_member_cannot_shadow (common.cj:10:24) | 未执行 |
| generic_owner_two_one | 1 | 未执行 | sema_extend_member_cannot_shadow (common.cj:7:24)<br>sema_extend_member_cannot_shadow (common.cj:10:24) | 未执行 |
| generic_owner_named | 1 | 未执行 | sema_param_named_mismatched (common.cj:7:24)<br>sema_param_named_mismatched (common.cj:10:24) | 未执行 |

## 3. 官方源码：确切执行次序

以下均为 `external/cangjie_compiler` 的 Git tag **v1.1.3**，不是当前 checkout v1.0.0：

- `src/Sema/CJMP/CheckCJMP.cpp:1193–1200`：
  按 collected specificDecls 顺序遍历非 broken、非 nominal 声明，调用 MatchSpecificDeclWithCommonDecls。
- `:1115–1155`：
  按 commonDecls 遍历；仅 matched=true 才 break；某个 MatchCJMPFunction 返回 false 后继续下一个候选。
- `:915–990`：
  候选先经过名字/外围 owner/泛型个数、映射后的函数类型、named 参数、双方默认值和构造器规则；
  最后才调用 TrySetSpecificImpl。早期验证产生的诊断不会因为后来找到了匹配者而自动等同“未发生”。
- `:898–912`：
  先检查 common.specificImplementation；已占用立即报告 sema_multiple_common_implementations 并 false。
  未占用才检查 NeedToReportMissingBody；通过后写指针和 doNotExport，并 true。
- `:730–737`：
  NeedToReportMissingBody 条件是 common 有 outerDecl、COMMON_WITH_DEFAULT、非 ABSTRACT，
  而 specific 为 ABSTRACT。并非所有“body 为空”的统一判断。
- `:387–445`：
  common extend key 包含 extended type、排序后的 inherited types、generic constraints。
  同 key 的 common extend 可归到一个 specific extend；同 key 的第二个 specific extend 本身会报告
  sema_specific_has_duplicate_extensions。这不允许通过两个同 key specific extend 规避函数重复。
- v1.2.0-alpha.20 `:1084–1088` / origin/main `:1191–1195`：
  多 parent 条件下改为 matched && !severalParents 才 break。该版本差异不能倒灌 1.1.3。

示意（省略之前的语义判据，保留状态顺序）：

```text
specific S2:
  common C1:
    signature compatible
    C1 already occupied -> MULTIPLE -> false
  common C2:
    signature compatible
    C2 free
    missing-body? -> report + false
    otherwise bind C2 -> true -> stop in 1.1.3
```

因此框架表示需要保留有序候选及阶段性判据，才能在检查器重放时恢复占用/缺体顺序；
简单把所有 specific 都固定到首个类型兼容 common 再只做 groupBy，不能表达上述源码规则。
本轮只有单 common 的占用/缺体分支得到 cjc 实测，C1→C2 分支依据为官方代码而非伪造的成功探针。

## 4. 仍未完成的 P0 证据

- 没有找到并实际编译出合法的“两个完全兼容 common、两个 specific”的单 common-part 程序。
- 还没有运行更早 alpha 二进制；早期泛型/default/后检查变化仅以 Git 源码取证。
- 没有声称已穷尽全部预发布版本的其他 CJMP 行为。精确已知引入提交补充到
  `../VERSION-EVIDENCE-20260929.md`，其余语义差异仍是 P0 待细化项。

