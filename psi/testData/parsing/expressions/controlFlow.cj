// 控制流表达式：if / else if / else 链、if 作为表达式、while、for-in、let 模式与 try。
// 官方依据：Cangjie 文档《流程控制》《异常处理》；cjc 1.0.5 / 1.1.3 零诊断。
func classify(n: Int64): String {
    if (n > 0) {
        return "positive"
    } else if (n < 0) {
        return "negative"
    } else {
        return "zero"
    }
}

func ifExpr(flag: Bool, a: Int64, b: Int64): Int64 {
    return if (flag) { a } else { b }
}

func loops(n: Int64): Int64 {
    var total = 0
    var i = 0
    while (i < n) {
        total += i
        i++
    }
    for (j in 1..=n) {
        total += j
    }
    for (k in 0..n:2) {
        total += k
    }
    return total
}

func letPattern(o: ?Int64): Int64 {
    var t = 0
    if (let v <- o) {
        t = v.getOrThrow()
    }
    while (let v <- o) {
        t += v.getOrThrow()
        break
    }
    return t
}

func tryCatch(): Int64 {
    try {
        return 1
    } catch (e: IllegalArgumentException) {
        return 2
    } finally {
        let _ = 0
    }
}
