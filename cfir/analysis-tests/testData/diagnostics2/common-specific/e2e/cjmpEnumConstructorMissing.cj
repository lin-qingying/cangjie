// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// common enum 构造器在 specific 缺失：common 方向 NOT_MATCHED（锚构造器，官方 common.cj:4:11）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum E {
    | A | <!NOT_MATCHED!>B<!>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific enum E {
    | A
}
