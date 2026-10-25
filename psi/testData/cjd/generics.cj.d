// 无体声明（四）：泛型类 / 接口的约束与无体成员。
// 官方依据：Cangjie 文档《泛型》；docs/cjd-declaration-file-support-v3.md。
interface Container<T> {
    func get(): T
    func put(value: T): Unit
}

class Box<T> where T <: Container<T> {
    private var value: T

    public init(value: T)
    public func get(): T
}

func maxOf<T>(a: T, b: T): T where T <: Comparable<T>

interface MultiBound<T> where T <: Container<T> & Comparable<T> {
    func compare(other: T): Int64
}
