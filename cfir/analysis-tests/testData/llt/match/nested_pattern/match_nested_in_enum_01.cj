


let val = 12

interface I {
    func get(): Int64
}

class A <: I {
    public func get(): Int64 {
        val
    }
}

enum E<T> {
    None | EE(T)

    func test() {
        match (this) {
            case None => 0
            case EE(v: I) => v.get()
            case _ => 0
        }
    }
}

main() {
    let v = EE(A()).test()
    if (v != val) {
        return 1
    }
    return 0
}
