// 复合赋值：算术、位运算与移位复合赋值。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
func compoundAssign(a: Int64, b: Int64): Int64 {
    let shifted = (a << 1) | (a >> 2)
    var x = a
    x += b
    x -= b
    x *= b
    x /= b
    x %= b
    x &= b
    x |= b
    x ^= b
    x <<= 1
    x >>= 1
    return x + Int64(shifted)
}
