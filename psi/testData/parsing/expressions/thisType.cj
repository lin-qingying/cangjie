// This 类型：返回 This 的方法与 extend 中的 This 用法。
// 官方依据：Cangjie 文档《this 类型》；cjc 1.0.5 / 1.1.3 零诊断。
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
