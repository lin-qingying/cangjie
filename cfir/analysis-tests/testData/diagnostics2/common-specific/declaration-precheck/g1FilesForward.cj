// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: 20261113-c3-declaration-group-probes/g1-files-forward-sema-witness；只见证 Sema。

// MODULE: common
// CJMP_MODE: COMMON
// FILE: marker.cj
package c3_group
public class Marker {}

// FILE: common-owner.cj
package c3_group
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
// FILE: specific-owner.cj
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

// FILE: independent.cj
package c3_group
public func unrelated(): Int64 { 0 }
