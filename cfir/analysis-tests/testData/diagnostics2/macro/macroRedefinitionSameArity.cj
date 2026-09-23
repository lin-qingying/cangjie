// RUN_PIPELINE_TILL: FRONTEND
macro package p

// 官方判据（PreCheck.cpp:1405-1430）：恰好 2 个同名宏且参数个数相等 → 两个都报；
// 诊断锚定宏名 token（cjc 1.0.5/1.1.3 实测）。Tokens 需 std.compiler，测试环境未引入，
// UNDECLARED_TYPE_NAME 与本用例无关。
public macro <!EXPAND_MACRO_REDEFINITION!>m<!>(t: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    t
}

public macro <!EXPAND_MACRO_REDEFINITION!>m<!>(u: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    u
}
