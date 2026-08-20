internal import std.collection.HashMap

class B {
    func use<T>(v: T, exec: (T) -> Unit) {
        exec(v)
    }
}

class A {
    private static let a = {
        =>
        let map = HashMap<String, (Array<Any>, () -> Any) -> Any>()
        register(B())
        map
    }()

    public static func register(aspect: A) {
        register("test", {v: Array<Any>, fn: () -> Any => ()})
    }
    public static func register(b: B) {
        b.use<A>(A(), register)
    }
    public static func register(name: String, fn: (Array<Any>, () -> Any) -> Any): Unit {
        a[name] = fn
    }
}

main() {}
