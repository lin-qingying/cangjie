// RUN_PIPELINE_TILL: FRONTEND
// 最后一个具名形参标识下标赋值签名；错误名称由共享 CFIR checker 锚定到完整形参名 token。
class Vec {
    private let data: Array<Int64> = [0]

    operator func [](index: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64): Unit {
        this.data[index] = v
    }
}
