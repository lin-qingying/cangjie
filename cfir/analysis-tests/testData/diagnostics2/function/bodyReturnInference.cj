// RUN_PIPELINE_TILL: FRONTEND

func acceptInt(x: Int64) {}

func acceptUnit(x: Unit) {}

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
    ""
}

func inferUnit() {
    let x = 1
}

func <!UNABLE_TO_INFER_RETURN_TYPE!>inferConflict<!>(flag: Bool) {
    if (flag) {
        return 1
    }
    ""
}

func declaredOk(): Int64 {
    1
}

func declaredBoolMismatch(): Bool {
    <!RETURN_TYPE_MISMATCH!>1<!>
}

func declaredUnitMismatch(): Unit {
    <!RETURN_TYPE_MISMATCH!>1<!>
}

main() {
    acceptInt(inferTail())
    acceptInt(inferReturn())
    acceptInt(inferJoined(true))
    acceptInt(unreachableTail())
    acceptUnit(inferUnit())
    acceptInt(declaredOk())
    0
}
