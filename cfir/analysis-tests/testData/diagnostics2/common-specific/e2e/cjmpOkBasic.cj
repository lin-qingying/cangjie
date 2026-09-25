// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// 正例：class/struct/顶层函数/enum 全部配对，零诊断
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Repo {
    public common init()
    public common func get(): Int64
}

public common struct Point {
    public common init()
}

public common func platform(): String

public common enum Color {
    | Red | Green
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Repo {
    public specific init() {}
    public specific func get(): Int64 { 1 }
}

public specific struct Point {
    public specific init() {}
}

public specific func platform(): String {
    "spec"
}

public specific enum Color {
    | Red | Green
}
