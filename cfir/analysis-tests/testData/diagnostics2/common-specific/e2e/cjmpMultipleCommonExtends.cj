// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 two-stage probe: .workbuddy/tmp/cjmp_probe113/multi_extend_common.cj + multi_extend_specific.cj
// 同一 specific extend 合并两个同 key common extend；第二个 owner 的 generic 成员仍必须匹配。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Box<T> {
    public common init()
}

common extend<T> Box<T> {
    public common func first(value: T): T { value }
}

common extend<T> Box<T> {
    public common func second(value: T): T
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Box<U> {
    public specific init() {}
}

specific extend<V> Box<V> {
    public specific func second(value: V): V { value }
}
