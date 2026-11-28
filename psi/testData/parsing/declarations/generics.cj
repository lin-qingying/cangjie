// 泛型与类型别名：泛型形参、where 约束、多上界、type 别名。
// 官方依据：Cangjie 文档《泛型》《类型别名》；cjc 1.0.5 / 1.1.3 零诊断。
type IntAlias = Int64
type Handler = (Int64) -> Int64

interface Comparable<T> {
    operator func <(other: T): Bool
}

interface Equatable<T> {
    operator func ==(other: T): Bool
}

class Pair<A, B> {
    public let first: A
    public let second: B

    public init(first: A, second: B) {
        this.first = first
        this.second = second
    }
}

func maxOf<T>(a: T, b: T): T where T <: Comparable<T> {
    if (a < b) {
        return b
    }
    return a
}

class Box<T> where T <: Comparable<T> {
    private var value: T

    public init(value: T) {
        this.value = value
    }

    public func get(): T {
        return this.value
    }
}

interface MultiBound<T> where T <: Comparable<T> & Equatable<T> {
    func compare(other: T): Int64
}
