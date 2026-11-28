// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：extend 内置类型上定义与内建同名的 operator（官方 sema_operator_overload_built_in_binary_operator "operator func +(Int64) of type Int64 is a built-in function"，锚 operator 声明）。
// 官方 cjc 1.0.5 / 1.1.3：sema_operator_overload_built_in_binary_operator。
extend Int64 {
    public operator func + (o: Int64): Int64 { return this }
}
