// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class PropertyProperty {
    public common init()
    public prop value: Int64 { get() { 1 } }
}

public common class VariableVariable {
    public common init()
    public var value: Int64 = 1
}

public common class PropertyToVariable {
    public common init()
    public prop <!REDECLARATION!>value<!>: Int64 { get() { 1 } }
}

public common class VariableToProperty {
    public common init()
    public var value: Int64 = 1
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

public specific class PropertyProperty {
    public specific init() {}
    public prop <!REDECLARATION!>value<!>: Int64 { get() { 2 } }
}

public specific class VariableVariable {
    public specific init() {}
    public var <!REDECLARATION!>value<!>: Int64 = 2
}

public specific class PropertyToVariable {
    public specific init() {}
    public var value: Int64 = 2
}

public specific class VariableToProperty {
    public specific init() {}
    public prop <!REDECLARATION!>value<!>: Int64 { get() { 2 } }
}
