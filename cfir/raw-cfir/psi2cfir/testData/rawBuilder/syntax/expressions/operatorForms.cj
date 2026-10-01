// RUN_PIPELINE_TILL: FRONTEND
// 运算符：优先级与结合性（官方《操作符》附录：数字越小优先级越高）、一元 / 二元 / 复合赋值、区间、比较、??、|>、字符串 +。
// 官方依据：Cangjie 文档《操作符》《整数类型》（** 的右操作数类型约束）；cjc 1.0.5 / 1.1.3 零诊断。
func identity(x: Int64): Int64 {
    return x
}

func operatorForms(a: Int64, b: Int64, flag: Bool, opt: ?Int64, s: String): Unit {
    let neg = -a
    let bitNot = !a
    let logicalNot = !flag
    let power = a ** UInt64(2)
    let powerRight = a ** UInt64(3)
    let mul = a * b
    let div = a / b
    let mod = a % b
    let add = a + b
    let sub = a - b
    let chainLeft = a - b - a
    let shl = a << b
    let shr = a >> b
    let cmp = a < b
    let cmpLe = a <= b
    let cmpGt = a > b
    let cmpGe = a >= b
    let eq = a == b
    let ne = a != b
    let and = a & b
    let xor = a ^ b
    let or = a | b
    let bitChain = (a ^ (b & a)) | b
    let logicalAnd = flag && (a > b)
    let logicalOr = flag || (a > b)
    let shortCircuit = flag && (a / b > 0)
    let coalesce = opt ?? 0
    let coalesceRight = opt ?? a
    let pipe = a |> identity
    let pipeChain = a |> identity |> identity
    let concat = s + "x"
    let range = 0..b
    let rangeClosed = 0..=b
    let mixed = a + b * 2 - a / 2 % 3
    let mixedShift = (a << 1) | (b >> 1) & 0xF
    let _ = (neg, bitNot, logicalNot, power, powerRight, mul, div, mod, add, sub, chainLeft, shl, shr)
    let _ = (cmp, cmpLe, cmpGt, cmpGe, eq, ne, and, xor, or, bitChain, logicalAnd, logicalOr, shortCircuit)
    let _ = (coalesce, coalesceRight, pipe, pipeChain, concat, range, rangeClosed, mixed, mixedShift)
}
