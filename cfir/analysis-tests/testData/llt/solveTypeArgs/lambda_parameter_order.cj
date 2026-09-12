func firstMismatch() {
    let f: (Int32, Int32) -> Int32 = {<!TYPE_MISMATCH!>a: Int16<!>, b => a + b}
}

func lastMismatch() {
    let f: (Int32, Int32) -> Int32 = {a, <!TYPE_MISMATCH!>b: Int16<!> => a <!INVALID_BINARY_OPERATOR!>+<!> b}
}

func independentBodyError() {
    let f: (Int32, Int32) -> Int32 = {<!TYPE_MISMATCH!>a: Int16<!>, b => <!UNRESOLVED_REFERENCE!>missingName<!>}
}

func independentStatementError() {
    let f: (Int32, Int32) -> Int32 = {<!TYPE_MISMATCH!>a: Int16<!>, b => <!UNRESOLVED_REFERENCE!>missingName<!>; 0}
}

func wrongCount() {
    let f: (Int32) -> Int32 = {<!PARAM_COUNT_MISMATCH!>a, b<!> => <!UNRESOLVED_REFERENCE!>missingName<!>}
}

func wrongCountReturn() {
    let f: (Int32) -> Int32 = {<!PARAM_COUNT_MISMATCH!>a, b<!> => <!TYPE_MISMATCH!>()<!>}
}

func knownPrefixReturn() {
    let f: (Int32, Int32) -> Int32 = {a, <!TYPE_MISMATCH!>b: Int16<!> => <!TYPE_MISMATCH!>()<!>}
}
