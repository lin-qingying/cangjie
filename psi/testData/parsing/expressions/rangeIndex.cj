// 区间与下标表达式：.. / ..=、带步长区间、下标读取与赋值、切片。
// 官方依据：Cangjie 文档《区间类型》《数组类型》；cjc 1.0.5 / 1.1.3 零诊断。
class Grid {
    private var cells: Array<Int64> = [0, 1, 2, 3, 4, 5]

    public init() {}

    public operator func [](index: Int64): Int64 {
        return cells[index]
    }

    public operator func [](index: Int64, value!: Int64): Unit {
        cells[index] = value
    }
}

func ranges(n: Int64): Int64 {
    var total = 0
    for (i in 1..10) {
        total += i
    }
    for (i in 1..=10) {
        total += i
    }
    for (i in 1..10:2) {
        total += i
    }
    for (i in 10..=1:-2) {
        total += i
    }
    let grid = Grid()
    total += grid[0]
    grid[0] = 100
    total += grid[0]
    return total + n
}
