// RUN_PIPELINE_TILL: FRONTEND
// This 类型：只能作为实例函数的返回类型（类的成员函数、extend 块的成员函数、struct 形参类型都不能用 This；见 recovery）。
// 官方依据：官方 ParseType.cpp parse_this_type_not_allow（"'This' type can only be used as the return type of an instance function"）；
// cjc 1.0.5 / 1.1.3 零诊断。
class Node {
    public init() {}

    public func me(): This {
        return this
    }
}

extend Node {
    public func alsoMe(): Node {
        return this
    }
}

func thisTypeReturns(): Unit {
    let n = Node()
    let a = n.me()
    let b = n.alsoMe()
    let _ = (a, b)
}
