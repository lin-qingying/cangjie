// invoke 操作符 —— struct 上定义和使用

struct Counter {
    let value: Int64 = 0

    public operator func ()(): Int64 {
        return value
    }

    public operator func ()(n: Int64): Int64 {
        return value + n
    }
}

struct Empty {}

func testStructInvokeNoArgs(): Unit {
    let c = Counter()
    c()
}

func testStructInvokeWithArg(): Unit {
    let c = Counter()
    c(10)
}

func testEmptyStructNoInvoke(): Unit {
    let e = Empty()
    <!NO_MATCHING_OPERATOR_INVOKE!>e<!>()
}
