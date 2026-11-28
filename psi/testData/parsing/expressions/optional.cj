// 可选类型表达式：?T 标注、可选链 a?.b / a?.f() / a?[i]、链式访问与 ? 后缀。
// 官方依据：Cangjie 文档《Option 类型》；cjc 1.0.5 / 1.1.3 零诊断。
class Node {
    public var value: Int64 = 0
    public var next: ?Node = None

    public init() {}

    public func get(): Int64 {
        return value
    }
}

func optionalChain(head: ?Node): Int64 {
    let first = head?.value
    let viaCall = head?.get()
    let deep = head?.next?.next?.value
    let _ = first
    let _ = viaCall
    return deep ?? 0
}

func optionalParams(x: ?Int64): Int64 {
    let doubled = x ?? 0
    return doubled + 1
}

func buildChain(): ?Node {
    let a = Node()
    let b = Node()
    a.next = b
    b.next = a
    return a
}
