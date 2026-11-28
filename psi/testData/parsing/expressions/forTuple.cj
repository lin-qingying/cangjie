// for 遍历形态：区间、带步长、元组解构与数组下标遍历。
// 官方依据：Cangjie 文档《流程控制》；cjc 1.0.5 / 1.1.3 零诊断。
func forTuple(pairs: Array<(Int64, String)>): Int64 {
    var total = 0
    for ((k, v) in pairs) {
        total += k + v.size
    }
    return total
}

func forIndexed(xs: Array<Int64>): Int64 {
    var total = 0
    for (i in 0..xs.size) {
        total += i + xs[i]
    }
    return total
}

func forRange(n: Int64): Int64 {
    var total = 0
    for (i in 0..n) {
        total += i
    }
    for (i in 1..=n:2) {
        total += i
    }
    return total
}
