// RUN_PIPELINE_TILL: FRONTEND
// 下标与切片：a[i]、a[i..j]、a[i..=j]、链式下标 matrix[0][1]、下标后成员访问 items[0].name、数组元素（类字段）赋值与自增。
// 注意：数组元素是 struct（值类型）时不能对其字段赋值（官方 sema_cannot_assign_to_immutable），这里用类元素。
// 官方依据：Cangjie 文档《数组类型》《区间操作符》；cjc 1.0.5 / 1.1.3 零诊断。
class Item {
    var id: Int64 = 0
    var name: String = "x"
}

func indexAndSliceForms(matrix: Array<Array<Int64>>, items: Array<Item>, s: String): Unit {
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
