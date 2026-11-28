// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C07 与 e5_probe.cj）：
// 构造器里的非限定 common let 赋值、复合赋值、自增自减使用 CJMP 诊断；成员访问使用通用不可变诊断。
package cjmp_p

public common open class M {
    public common let v: Int64
    public common let initialized: Int64 = 0
    public common var mutable: Int64 = 0
    public common init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>v<!> = 1
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>initialized<!> = 2
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = 2
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>v<!> += 1
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> += 1
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>v++<!>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.v++<!>
        mutable = 3
        mutable += 1
        mutable++
    }

    public common func mutateOutsideConstructor(): Unit {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = 4
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>v++<!>
    }
}

public common class Child <: M {
    public common init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>v<!> = 5
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = 6
    }
}

public common class ExplicitReceiverFirstWrite {
    public common let value: Int64

    public common init() {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>value<!> = 1
    }
}

public common class VArrayHolder {
    public common let values: VArray<Int64, $1> = [0]
    public common var mutableValues: VArray<Int64, $1> = [0]

    public common init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>values[0]++<!>
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>values[0]--<!>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.values[0]++<!>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.values[0]--<!>
        mutableValues[0]++
        mutableValues[0]--
    }
}

public common struct S {
    public common let field: Int64
    public common var mutable: Int64 = 0

    public common init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>field<!> = 1
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>field<!> = 2
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>field<!> += 1
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>field<!> += 1
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>field++<!>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.field++<!>
        mutable = 3
        mutable += 1
        mutable++
    }
}
