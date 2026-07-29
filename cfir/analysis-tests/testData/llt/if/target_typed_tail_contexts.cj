
func int32Value(): Int32 {
    return 1
}

struct TargetTypedHolder {
    public var fieldBad: Int32 = 1
    public var fieldValue: Int64 = if (true) {
        1
    } else {
        <!TYPE_MISMATCH!>fieldBad<!>
    }
}

func defaultParam(value!: Int64 = if (true) {
    1
} else {
    <!TYPE_MISMATCH!>int32Value()<!>
}): Unit {}

func returnIf(flag!: Bool): Int64 {
    return if (flag) {
        1
    } else {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func bodyIf(flag!: Bool): Int64 {
    if (flag) {
        1
    } else {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func assignmentIf(flag!: Bool): Unit {
    var value: Int64 = 0
    value = if (flag) {
        1
    } else {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func matchRhs(value!: Int64): Unit {
    var result: Int64 = 0
    result = match (value) {
        case 0 => 1
        case _ => <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func matchInitializer(value!: Int64): Unit {
    let result: Int64 = match (value) {
        case 0 => 1
        case _ => <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func returnMatch(value!: Int64): Int64 {
    return match (value) {
        case 0 => 1
        case _ => <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func tryRhs(): Unit {
    var result: Int64 = 0
    result = try {
        1
    } catch (_) {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func tryInitializer(): Unit {
    let result: Int64 = try {
        1
    } catch (_) {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func returnTry(): Int64 {
    return try {
        1
    } catch (_) {
        <!TYPE_MISMATCH!>int32Value()<!>
    }
}

func nestedIfTail(flag!: Bool): Unit {
    let value: Int64 = if (flag) {
        if (flag) {
            1
        } else {
            <!TYPE_MISMATCH!>int32Value()<!>
        }
    } else {
        2
    }
}

func nonTailIfInBlock(flag!: Bool): Unit {
    let value: Int64 = if (flag) {
        if (flag) {
            int32Value()
        }
        1
    } else {
        2
    }
}

func noExpectedIf(flag!: Bool): Unit {
    let value = if (flag) {
        int32Value()
    } else {
        int32Value()
    }
}

func noElseIf(flag!: Bool): Unit {
    let value: Int64 = <!TYPE_MISMATCH!>if (flag) {
        int32Value()
    }<!>
}

main(): Int64 {
    TargetTypedHolder()
    defaultParam()
    assignmentIf(flag: true)
    matchRhs(value: 0)
    matchInitializer(value: 0)
    tryRhs()
    tryInitializer()
    nestedIfTail(flag: true)
    nonTailIfInBlock(flag: true)
    noExpectedIf(flag: true)
    noElseIf(flag: true)
    returnMatch(value: 0)
    returnTry()
    return returnIf(flag: true)
}
