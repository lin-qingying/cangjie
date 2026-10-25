// 自增自减：局部变量、数组成员、对象字段与 this 限定字段。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
class Counter {
    var count: Int64 = 0
    var other: Int64 = 0

    public init() {}

    public func bump(): Int64 {
        count++
        this.count--
        return count
    }
}

struct Holder {
    var items: Array<Int64> = [0, 0]
}

func incDec(xs: Array<Int64>): Unit {
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
