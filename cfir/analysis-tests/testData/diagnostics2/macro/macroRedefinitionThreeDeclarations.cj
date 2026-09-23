// RUN_PIPELINE_TILL: FRONTEND
macro package p

// 官方 size>2 分支在 1.0.5/1.1.3 双 SDK 实测均不报（上游去重吞掉），按可观测行为对齐。
// Tokens 需 std.compiler，测试环境未引入，UNDECLARED_TYPE_NAME 与本用例无关。
public macro m(t: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    t
}

public macro m(u: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    u
}

public macro m(v: <!UNDECLARED_TYPE_NAME!>Tokens<!>): <!UNDECLARED_TYPE_NAME!>Tokens<!> {
    v
}
