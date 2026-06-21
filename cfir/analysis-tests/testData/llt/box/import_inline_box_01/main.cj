// FILE: pkg.cj


package pkg

interface I {
    func test(): Int64
}

extend Object <: I {
    public func test() {
        222
    }
}

extend Int64 <: I {
    public func test(): Int64 {
        0
    }
}

extend Int8 <: I {
    public func test(): Int64 {
        0
    }
}

public class A {
    var vv: ?I = None
    public func setObj(v: Object) {
        vv = v
    }
    public func set(v: Int64) {
        vv = v
    }
    public func run() {
        // Avoid ast inline
        if (1 > 2) {
            println("a")
            println("a")
            println("a")
            println("a")
            println("a")
            println("a")
            println("a")
            println("a")
            println("a")
        }
        let i = vv.getOrThrow()
        let aa = i.test()
        return aa
    }
}

public func getA() {
    return A()
}

// FILE: main.cj


internal import pkg.*

main() {
    let a = getA()
    a.setObj(a)
    if (a.run() != 222) {
        return 1
    }
    return 0
}
