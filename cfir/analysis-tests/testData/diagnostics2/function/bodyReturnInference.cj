// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS

func acceptInt(x: Int64) {
    let _ = x
}

func acceptUnit(x: Unit) {
    let _ = x
}

func inferTail() {
    1
}

func inferReturn() {
    return 1
}

func inferJoined(flag: Bool) {
    if (flag) {
        return 1
    }
    2
}

func unreachableTail() {
    return 1
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>""<!>
}

func inferUnit() {
    let x = 1
    let _ = x
}

func inferConflict(flag: Bool) {
    if (flag) {
        return 1
    }
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>""<!>
}

func declaredOk(): Int64 {
    1
}

func declaredBoolMismatch(): Bool {
    <!CANNOT_CONVERT_LITERAL!>1<!>
}

func declaredUnitTailExpression(): Unit {
    1
}

main(): Unit {
    acceptInt(inferTail())
    acceptInt(inferReturn())
    acceptInt(inferJoined(true))
    acceptInt(unreachableTail())
    acceptUnit(inferUnit())
    acceptInt(declaredOk())
}
