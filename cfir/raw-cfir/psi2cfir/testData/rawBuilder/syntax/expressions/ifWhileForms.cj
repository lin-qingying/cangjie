// RUN_PIPELINE_TILL: FRONTEND
// if / while / do-while / break / continue / return：if-else-if 链、while 条件里的 let 模式解构（先取值再解构）、do-while、
// 循环体内嵌套控制流、for 遍历数组。
// 官方依据：Cangjie 文档《控制流》；cjc 1.0.5 / 1.1.3 零诊断。
func cond(x: Int64): Bool {
    return x > 0
}

func ifWhileForms(xs: Array<Int64>, o: ?Int64): Int64 {
    var total = 0
    if (cond(1)) {
        total = 1
    } else if (cond(2)) {
        total = 2
    } else if (cond(3)) {
        total = 3
    } else {
        total = 4
    }
    var i = 0
    while (i < 10) {
        i++
        if (i == 3) {
            continue
        }
        if (i == 8) {
            break
        }
        total += i
    }
    do {
        i--
    } while (i > 5)
    var rest = o
    while (rest != None) {
        total += rest.getOrThrow()
        rest = None
    }
    if (let Some(v) <- o) {
        total += v
    }
    for (x in xs) {
        total += x
    }
    if (total > 100) {
        return total
    }
    return total
}
