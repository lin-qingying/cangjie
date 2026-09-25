// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// common extend 与 specific extend 按扩展类型配对，成员配对，零诊断
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class C {
    public common init()
}
common extend C {
    public common func f(): Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class C {
    public specific init() {}
}
specific extend C {
    public specific func f(): Int64 { 1 }
}
