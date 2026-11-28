// 算术与赋值表达式：四则运算、幂、取余、自增自减、复合赋值、一元运算。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
func arithmetic(a: Int64, b: Int64, f: Float64): Int64 {
    let sum = a + b
    let diff = a - b
    let prod = a * b
    let quot = a / b
    let rem = a % b
    let power = a ** UInt64(b)
    let negated = -a
    let inverted = !a
    var acc = sum
    acc += diff
    acc -= prod
    acc *= 2
    acc /= 2
    acc %= 3
    acc++
    acc--
    let shifted = (acc << 1) >> 1
    let masked = (acc & 0xFF) | 0x0F
    let flipped = acc ^ 0xFF
    let _ = f + 1.0
    return acc + shifted + masked + flipped
}
