// RUN_PIPELINE_TILL: FRONTEND
// 赋值：普通赋值、全部复合赋值（+= -= *= /= %= **= <<= >>= &= ^= |= &&= ||=）、多值赋值（含通配符与交换）、成员字段赋值。
// 官方依据：Cangjie 文档《基本操作符》「赋值操作符 / 复合赋值操作符」；cjc 1.0.5 / 1.1.3 零诊断。
class Counter {
    var count: Int64 = 0
}

func assignmentForms(): Unit {
    var a: Int64 = 1
    a = 2
    a += 1
    a -= 1
    a *= 2
    a /= 2
    a %= 7
    a **= 2
    a <<= 1
    a >>= 1
    a &= 7
    a |= 8
    a ^= 9
    var b: Bool = true
    b &&= false
    b ||= true

    var x: Int64 = 0
    var y: Int64 = 0
    (x, y) = (1, 2)
    (x, y) = (y, x)
    (x, _) = (3, 4)
    (_, _) = (5, 6)

    let c = Counter()
    c.count = 1
    c.count += 2
}
