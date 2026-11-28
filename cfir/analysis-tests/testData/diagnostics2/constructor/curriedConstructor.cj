// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`class C { public init(x: Int64)(y: Int64) {} }`
//   → error: constructor cannot have more than one parameter list（锚第一个参数列表的左括号，2:16）。
// 官方实现 TypeChecker::CheckConstructor：仓颉构造器不支持柯里化，第二个参数列表即非法；
// 诊断锚在首个参数列表左括号，CFIR `checkCurriedConstructor` 与之一致。
class Curried {
    public init<!CANNOT_CURRYING!>(<!>x: Int64)(y: Int64) {
    }
}

class Normal {
    public init(x: Int64) {
    }
}
