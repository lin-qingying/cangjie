// RUN_PIPELINE_TILL: FRONTEND
// 位运算与复合赋值全套：`& | ^ << >>`、以及 `&= |= ^= <<= >>=` 与算术复合赋值（`+= -= *= /= %=`）连用；移位链 `(a << 1) | (a >> 2)`。
// 官方依据：Cangjie 文档《操作符》《赋值》；cjc 1.0.5 / 1.1.3 零诊断。
func bitwiseCompoundAssignForms(a: Int64, b: Int64): Int64 {
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

func bitwiseShiftForms(a: UInt64, b: UInt64): UInt64 {
    return (a << 1) | (a >> 2) ^ (b << 3) & (b >> 4)
}
