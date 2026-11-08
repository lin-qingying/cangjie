package test

// 源码 type 别名（仓颉语法是 `type`，不是 `typealias`）作为函数签名类型：
// 解析链路必须能把它解析为别名类型，且不产生任何诊断。
type MyInt = Int64

func take(x: MyInt): MyInt {
    return x
}
