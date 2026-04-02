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
    let v: Int64 = c()  // OK：调用无参 invoke
}

func testStructInvokeWithArg(): Unit {
    let c = Counter()
    let v: Int64 = c(10)  // OK：调用有参 invoke
}

func testEmptyStructNoInvoke(): Unit {
    let e = Empty()
    <!NO_MATCHING_OPERATOR_INVOKE!>e<!>()  // 错误：Empty 没有 invoke
}
