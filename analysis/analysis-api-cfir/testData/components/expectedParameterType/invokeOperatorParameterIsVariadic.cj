package completion.expectedparametertype

class Callable {
    // `()` 是官方例外里**允许**变长的两个运算符之一（`()` 与 `[]`）。
    public operator func ()(values: Array<Int64>): Int64 {
        0
    }
}

func invokeOperatorParameterIsVariadic() {
    let callable = Callable()
    // 走一次真实调用：形参类型只有在调用解析后才落到 CFIR 上，
    // 直接查声明符号会因为类型没解析而一律得到「不是变长」，那测的就不是本规则。
    callable(1, 2, 3)
}
