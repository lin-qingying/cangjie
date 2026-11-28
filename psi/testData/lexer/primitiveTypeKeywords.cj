// 关键字（二）：基本类型、变量声明与特殊关键字，全部置于合法语法位置。
// 官方依据：Cangjie 文档《关键字》《基本数据类型》；cjc 1.0.5 / 1.1.3 零诊断。
package primitivetypeforms

func primitives(
    a: Int8, b: Int16, c: Int32, d: Int64, e: IntNative,
    f: UInt8, g: UInt16, h: UInt32, i: UInt64, j: UIntNative,
    k: Float16, l: Float32, m: Float64,
): Float64 {
    let n: Bool = true
    let o: Rune = r'a'
    let p: Unit = ()
    let q: VArray<Int64, $2> = [1, 2]
    let _ = (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    return m
}

func never(): Nothing {
    throw Exception("unreachable")
}

const CONST_VAL: Int64 = 1
var VAR_VAL: Int64 = 2
let LET_VAL: Int64 = 3

class WithProp {
    public prop tag: Int64 {
        get() {
            return 1
        }
    }

    public init() {}

    public func who(): String {
        return "withprop"
    }
}

func useAll(): Int64 {
    let w = WithProp()
    return w.tag + CONST_VAL + VAR_VAL + LET_VAL
}
