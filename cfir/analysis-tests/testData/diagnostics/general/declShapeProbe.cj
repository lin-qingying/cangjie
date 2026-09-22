// RUN_PIPELINE_TILL: FRONTEND
//
// P3 批次 3（字面量/数组/range/模式 + 作用域与访问族）回归语料。官方语义以 cjc 1.0.5 探针取证：
//   - sema_invalid_enum_member_access：enum 变量不能作为成员访问 base 取 enum 构造器
//     （NameReferenceExpr.cpp:1085；官方锚整个成员访问 `e.A`，cjc JSON 只输出其首字符）。
//   - sema_pattern_can_not_be_assigned：var-with-pattern 的模式必须不可反驳
//     （TypeCheckDecl.cpp:279；锚整个声明模式）。
//   - 官方实测不触达（负证据，不实现）：array 具名实参族 3 项（resolver 具名实参诊断先行）、
//     ambiguous_expo_right_operand_type、inconsistency_range_elemType、range_step_not_int64、
//     wrong_forin_guard、pattern_literal_expected（镜像零发射点）、type_must_toplevel
//     （parse_unexpected_declaration_in_scope 先行）、illegal_this_in_interface
//     （invalid_this_call_outside_ctor 先行）、illegal_access_inner_classlike /
//     illegal_access_interface_field / import_not_in_current_module（镜像零发射点）、
//     fail_flow_expr_operand_has_named_param（invalid_binary_expr 先行）。
enum E { A | B }

func enumMemberAccess(e: E): Unit {
    let _ = <!INVALID_ENUM_MEMBER_ACCESS!>e.A<!>
    let _ = E.A
}

func refutablePattern(o: ?Int64): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>Some(v)<!> = o
    let _ = v
}

func irrefutablePattern(o: ?Int64): Unit {
    let (a, b) = (1, 2)
    let _ = a
    let _ = b
    let _ = o
}

main() {}
