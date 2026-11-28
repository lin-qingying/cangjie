// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数类型 / 元组类型的形参列表里具名（`name:`）与无名混写（官方 ParseType.cpp ParseTypeWithParen
// parse_all_parameters_must_be_named：要么全部具名、要么全部不具名）。官方按位置解析 `IDENTIFIER ':'`，所以只能是"具名在前"；
// 这里写"无名在前、具名在后"，该诊断随之触发。
// 官方 cjc 1.0.5 / 1.1.3：parse_all_parameters_must_be_named "in a parameter type list, either all parameters must be named, or none of them; mixed is not allowed"。
func mixed(g: (Int64, x: String) -> Bool): Bool {
    return g(1, "x")
}

type MixedTuple = (Int64, x: String)

