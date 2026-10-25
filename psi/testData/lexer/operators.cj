// 运算符与分隔符：全部置于合法语法位置（cjc 1.0.5 可编译验证）。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
// 说明：<-、~>、|>、...、::、@!、$ 前缀与宏转义 \[ \] \( \) \@ 等无独立合法宿主的
// token 形态见 lexer/operatorsTokenOnly.cj。
package operatorforms

@Deprecated
func annotated(): Unit {}

class Box2 {
    var v: Int64 = 0

    public init() {}

    public operator func [](index: Int64): Int64 {
        return index
    }
}

func add(a: Int64, b: Int64): Int64 {
    return a + b
}

func operatorForms(): Int64 {
    var a = 10
    var b = 3
    let arithmetic = 1 + 2 - 3 * 4 / 5 % 6
    a++
    a--
    b++
    let less = a < b
    let lessEq = a <= b
    let equal = a == b
    let notEq = a != b
    let greater = a > b
    let greaterEq = a >= b
    let logicalAnd = less && lessEq
    let logicalOr = equal || notEq
    let logicalNot = !logicalAnd
    let bitwiseAnd = a & b
    let bitwiseOr = a | b
    let bitwiseXor = a ^ b
    let shiftLeft = a << 1
    let shiftRight = a >> 1
    a = 1
    a += b
    a -= b
    a *= b
    a /= b
    a %= b
    a &= b
    a |= b
    a ^= b
    a <<= 1
    a >>= 1
    let arrow: () -> Unit = {
        => ()
    }
    let matchForm = match (a) {
        case 1 => 10
        case _ => 20
    }
    var rangeTotal = 0
    for (i in 1..10) {
        rangeTotal += i
    }
    for (i in 1..=10) {
        rangeTotal += i
    }
    let optType: ?Int64 = None
    let quest = optType ?? 0
    if (let Some(bound) <- optType) {
        a += bound
    }
    let box = Box2()
    let indexed = box[0]
    let braces: Array<Int64> = [1, 2]
    let parens = (a + b)
    let member = box.v
    let call = add(a, b)
    let _ = (arithmetic, less, lessEq, equal, notEq, greater, greaterEq)
    let _ = (logicalAnd, logicalOr, logicalNot, bitwiseAnd, bitwiseOr, bitwiseXor)
    let _ = (shiftLeft, shiftRight, arrow, matchForm, rangeTotal)
    let _ = (quest, indexed, braces, parens, member, call)
    return a + b
}

func subtypeForm<T>(x: T, y: T): T where T <: Comparable<T> {
    return if (x < y) {
        x
    } else {
        y
    }
}
