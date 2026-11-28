package source_control_flow

public interface ControlFlow {
    func walk(limit: Int64): Int64 {
        var total: Int64 = 0
        var index: Int64 = 0
        while (index < limit) {
            index++
            if (index == 2) {
                continue
            }
            if (index > 4) {
                break
            }
            total += index
        }
        do {
            total -= 1
        } while (total > limit)
        for (element in 0..limit) {
            if (element == 1) {
                continue
            } else {
                total += element
            }
            if (element >= 3) {
                break
            }
        }
        return total
    }

    // 最后一个 if 表达式是函数的值，不在测试源中添加 return。
    func choose(flag: Bool): Int64 {
        if (flag) {
            1
        } else {
            2
        }
    }
}
