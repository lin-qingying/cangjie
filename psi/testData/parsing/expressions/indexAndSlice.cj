// 下标与切片：多维下标、链式下标、半开 / 闭合切片与下标赋值。
// 官方依据：Cangjie 文档《数组类型》《字符串类型》；cjc 1.0.5 / 1.1.3 零诊断。
class Item {
    var id: Int64 = 0
    var name: String = "x"

    public init() {}
}

func indexAndSlice(matrix: Array<Array<Int64>>, items: Array<Item>, s: String): Unit {
    let cell = matrix[0][1]
    let itemId = items[0].id
    let itemName = items[0].name
    let sub = matrix[0]
    let sliceHalfOpen = sub[1..2]
    let sliceClosed = sub[1..=2]
    let sliceFromZero = s[0..1]
    matrix[0][1] = 5
    items[0].id = 7
    items[0].id++
    let _ = (cell, itemId, itemName, sliceHalfOpen, sliceClosed, sliceFromZero)
}
