// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: an unresolved constructor signature is unmatched in exhaustive enums; common constructors remain required under non-exhaustive enums.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum Exhaustive {
    | <!NOT_MATCHED!>Value(Int64)<!>
}

public common enum NonExhaustive {
    | <!NOT_MATCHED!>Value(Int64)<!> | ...
}

public common enum DefaultEnum {
    | Value
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific enum Exhaustive {
    | <!NOT_MATCHED!>Value(<!UNDECLARED_TYPE_NAME!>MissingType<!>)<!>
}

public specific enum NonExhaustive {
    | Value(<!UNDECLARED_TYPE_NAME!>MissingType<!>) | ...
}

func sourcePresenceOnly(): Unit { }
