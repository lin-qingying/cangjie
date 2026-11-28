// 数组与元组字面量：一维 / 嵌套数组、异构元组与元组嵌套。
// 官方依据：Cangjie 文档《数组类型》《元组类型》；cjc 1.0.5 / 1.1.3 零诊断。
func arrayTupleLiterals(): Unit {
    let arr = [1, 2, 3]
    let empty: Array<Int64> = []
    let nested = [[1, 2], [3, 4]]
    let mixed = [[1, 2], [3, 4], [5, 6]]
    let tup = (1, "hello", true)
    let nestedTuple = ((1, 2), (3, 4))
    let single: Int64 = 1
    let _ = (arr, empty, nested, mixed, tup, nestedTuple, single)
}
