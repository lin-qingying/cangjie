G.EXP.02 不要期望浮点运算得到精确的值

【级别】建议

【描述】

因为存储二进制浮点的 bit 位是有限的，所以二进制浮点数的表示范围也是有限的，并且无法精确地表示所有实数。因此，浮点数计算结果也不是精确值，除了可以表示为 2 的幂次以及整数数乘的浮点数可以准确表示外，其余数的值都是近似值。

实际编程中，要结合场景需求，尤其是对精度的要求，合理选择浮点数操作。

例如，对于浮点值比较，如果对比较精度有要求，通常不建议直接用 != 或 == 比较，而是要考虑对精度的要求。

【正例】

```cangjie
import std.math.*

func isEqual(a: Float64, b: Float64): Bool {
    return abs(a - b) <= 1e-6
}

func compare(x: Float64) {
    if (isEqual(x, 3.14)) {
        // CODE
    } else {
        // CODE
    }
}
```

【反例】

```cangjie
func compare(x: Float64) {
    if (x == 3.14) {
        // CODE
    } else {
        // CODE
    }
}
```