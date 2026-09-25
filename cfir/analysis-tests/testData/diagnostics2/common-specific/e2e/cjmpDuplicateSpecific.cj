// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7b.txt）；替换原单文件占位 fixture
// 同一 common 被两个 specific 绑定：重载冲突 + common 上 MULTIPLE_COMMON_IMPLEMENTATIONS + 第二个 specific NOT_MATCHED
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!MULTIPLE_COMMON_IMPLEMENTATIONS!>public common func f(): Int64<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func f(): Int64 { 1 }
<!NOT_MATCHED!>public specific func <!CONFLICTING_OVERLOADS!>f<!>(): Int64 { 2 }<!>
