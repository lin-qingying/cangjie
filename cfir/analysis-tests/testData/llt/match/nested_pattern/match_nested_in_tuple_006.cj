


let val = 12

interface I {
    func get(): Int64
}

class A <: I {
    public func get(): Int64 {
        val
    }
}

func test<T>(a: (Int64, T)) {
    match (a) {
        case (_, v: I) => v.get()
        case _ => 0
    }
}

main() {
    let v = test((1, A()))
    if (v != val) {
        return 1
    }
    return 0
}
