// 控制流：if / else、while、do-while、for-in、break / continue、throw / try / catch。
// 官方依据：Cangjie 文档《流程控制》《异常处理》；cjc 1.0.5 / 1.1.3 零诊断。
func controlFlow(n: Int64): Int64 {
    if (n > 0) {
        return 1
    } else if (n < 0) {
        return -1
    } else {
        return 0
    }
}

func loops(n: Int64): Int64 {
    var i = 0
    while (i < n) {
        i++
    }

    do {
        i--
    } while (i > 0)

    for (j in 0..n) {
        if (j == 3) {
            continue
        }
        if (j == 8) {
            break
        }
    }

    return i
}

func exceptions(): Unit {
    try {
        throw OverflowException()
    } catch (e: OverflowException) {
        return
    } finally {
        return
    }
}

func letIf(condition: Bool, a: Int64, b: Int64): Int64 {
    let result = if (condition) { a } else { b }
    return result
}
