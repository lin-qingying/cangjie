// invoke 操作符 —— 多重载和参数不匹配

class Handler {
    public operator func ()(a: Int64): Int64 {
        return a
    }

    public operator func ()(a: Int64, b: Int64): Int64 {
        return a + b
    }

    public operator func ()(s: Bool): Bool {
        return s
    }
}

func testCorrectOverloads(): Unit {
    let h = Handler()
    h(1)          // OK：匹配 (Int64): Int64
    h(1, 2)       // OK：匹配 (Int64, Int64): Int64
    h(true)       // OK：匹配 (Bool): Bool
}

func testOverloadArgMismatch(): Unit {
    let h = Handler()
    <!NO_MATCHING_OPERATOR_INVOKE!>h<!>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)  // 错误：Float64 不匹配任何重载
}

// 变量类型的 invoke 返回值参与类型检查
func testInvokeReturnInContext(): Unit {
    let h = Handler()
    h(1)                    // OK：返回 Int64
    h(true)                 // OK：返回 Bool
    let _: Int64 = <!NO_MATCHING_OPERATOR_INVOKE!>h<!>(<!ARGUMENT_TYPE_MISMATCH!>true<!>)  // 错误：invoke 返回值与期望类型不匹配
}
