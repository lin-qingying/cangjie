// RUN_PIPELINE_TILL: FRONTEND
func localReadBeforeAssign(): Int64 {
    let value: Int64
    return <!USED_BEFORE_INITIALIZATION!>value<!>
}

func localReadAfterBothBranches(flag: Bool): Int64 {
    var value: Int64
    if (flag) {
        value = 1
    } else {
        value = 2
    }
    return value
}

func localReadAfterSingleBranch(flag: Bool): Int64 {
    var value: Int64
    if (flag) {
        value = 1
    }
    return <!USED_BEFORE_INITIALIZATION!>value<!>
}

class FieldOrder {
    let first: Int64 = 1
    let second: Int64 = <!USED_BEFORE_INITIALIZATION!>third<!>
    let third: Int64 = 3
}

class FieldReadBeforeCtorAssignment {
    let value: Int64

    init() {
        let copy: Int64 = <!USED_BEFORE_INITIALIZATION!>value<!>
        value = copy + 1
    }
}
