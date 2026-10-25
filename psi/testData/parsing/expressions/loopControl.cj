// 循环控制：while / for 中的 break、continue 与嵌套循环。
// 官方依据：Cangjie 文档《流程控制》；cjc 1.0.5 / 1.1.3 零诊断。
func loopControl(n: Int64): Int64 {
    var total = 0
    var i = 0
    while (true) {
        i++
        if (i > n) {
            break
        }
        if (i % 2 == 0) {
            continue
        }
        total += i
    }
    for (j in 1..=n) {
        if (j % 3 == 0) {
            continue
        }
        if (j > 100) {
            break
        }
        total += j
    }
    return total
}

func nestedLoops(rows: Int64, cols: Int64): Int64 {
    var count = 0
    for (r in 1..=rows) {
        var c = 0
        while (c < cols) {
            c++
            if (c == r) {
                continue
            }
            count++
        }
    }
    return count
}
