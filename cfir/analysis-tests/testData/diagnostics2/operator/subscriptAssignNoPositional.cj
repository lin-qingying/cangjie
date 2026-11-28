// RUN_PIPELINE_TILL: FRONTEND
// 下标赋值要求至少一个位置索引参数；诊断锚定完整 `[]` 运算符名 token。
class Vec {
    private let data: Array<Int64> = [0]

    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(value!: Int64): Unit {
        this.data[0] = value
    }
}
