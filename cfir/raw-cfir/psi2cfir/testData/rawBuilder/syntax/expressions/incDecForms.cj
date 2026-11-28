// RUN_PIPELINE_TILL: FRONTEND
// 自增自减：只允许后缀形式；作用在局部变量、成员字段、下标元素上。
// 官方依据：Cangjie 文档《基本操作符》「自增自减操作符」（只有后缀）；cjc 1.0.5 / 1.1.3 零诊断（前缀形式是官方 parse 错误，见 recovery/prefixIncDec.cj）。
class Counter {
    var count: Int64 = 0
    var other: Int64 = 0

    public func bump(): Int64 {
        count++
        this.count--
        return count
    }
}

struct Holder {
    var items: Array<Int64> = [0, 0]
}

func incDecForms(xs: Array<Int64>): Unit {
    var a = 1
    a++
    a--
    a++
    xs[0]++
    xs[1]--
    let c = Counter()
    c.count++
    c.count--
    let h = Holder()
    h.items[0]++
    let _ = (a, c, h)
}
