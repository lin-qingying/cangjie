// RUN_PIPELINE_TILL: FRONTEND
macro package p

// 官方允许 attribute + non-attribute 配对：参数个数不等即放行，不报重定义。
// Tokens 需 std.compiler，测试环境未引入，UNDECLARED_TYPE_NAME 与本用例无关。
public macro m(t: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    t
}

public macro m(t: <!UNDECLARED_TYPE_NAME!>Tokens<!>, u: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    t
}
