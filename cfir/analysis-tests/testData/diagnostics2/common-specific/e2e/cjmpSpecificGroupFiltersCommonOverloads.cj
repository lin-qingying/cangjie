// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class OverloadBox {
    public common init()
    public func f(value: Int64): Unit { }
    public func f(value: String): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

public specific class OverloadBox {
    public specific init() { }
    <!NOT_MATCHED!>public specific func f(value: String): Unit { }<!>
}

public func callCommonOnlyOverload(box: OverloadBox): Unit {
    box.f(1)
}
