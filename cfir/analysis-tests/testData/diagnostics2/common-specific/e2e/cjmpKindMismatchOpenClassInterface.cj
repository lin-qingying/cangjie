// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7b.txt）；替换原单文件占位 fixture
// common open class 与 specific interface：同上（open 不影响种类判定）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!NOT_MATCHED!>public common open class Service {
    <!NOT_MATCHED!>public common init()<!>
}<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific interface Service {}<!>
