// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: 20261113-c3-declaration-group-probes/g1-baseline-sema-witness；G1 无 witness 的 exportID ICE 不算成功。

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package c3_group
public class Marker {}
public common class Box {
    public common init()
    public common func f(value: Marker): Int64 {
        let _ = value
        11
    }
    public common func f(value: Int64): Int64 {
        value + 22
    }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package c3_group
public specific class Box {
    public specific init() {}
    public specific func f(value: Marker): Int64 {
        let _ = value
        33
    }
    public func f(value: Int64): Int64 {
        value + 44
    }
    <!NOT_MATCHED!>public specific func unmatchedWitness(): Unit {}<!>
}
