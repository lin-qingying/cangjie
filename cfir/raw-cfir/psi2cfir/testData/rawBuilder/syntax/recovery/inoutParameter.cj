// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：普通函数的形参写 `inout T`（官方 inout 只出现在 C 互操作形参声明、宏的 inout 形参与调用实参上，不是普通函数的形参类型）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_type "expected type name after ':', found keyword 'inout'" / parse_expected_decl。
func inoutParameter(x: inout Int64): Unit {
    x = 1
}

func inoutCallSite(a: Array<Int64>): Unit {
    takes(a[0])
}

func takes(x: Int64): Unit {}
