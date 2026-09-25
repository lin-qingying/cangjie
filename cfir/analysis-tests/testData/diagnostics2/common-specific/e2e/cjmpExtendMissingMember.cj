// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// specific extend 缺成员：common 方向 NOT_MATCHED 锚 common extend 成员（官方 common.cj:7:5）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class C {
    public common init()
}
common extend C {
    <!NOT_MATCHED!>public common func f(): Int64<!>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class C {
    public specific init() {}
}
specific extend C {
}
