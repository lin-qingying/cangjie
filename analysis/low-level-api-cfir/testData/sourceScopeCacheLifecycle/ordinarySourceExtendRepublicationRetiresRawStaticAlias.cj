package source_scope_harness

interface Root<T> {
    func value(x: T): T
}

interface Left<T> <: Root<T> {}
interface Right<T> <: Root<T> {}

class Leaf<T> <: Left<T> & Right<T> {
    public init() {}
    public func value(x: T): T { x }
}

extend<T> Leaf<T> {
    public func extensionValue(x: T): T { value(x) }
    public static func staticMarker(): Int64 { 17 }
}

type LeafInt = Leaf<Int64>

func instanceUse(x: LeafInt, v: Int64): Int64 {
    x.value(v) + x.extensionValue(v)
}

func staticUse(): Int64 {
    let x = LeafInt()
    x.value(1) + LeafInt.staticMarker()
}
