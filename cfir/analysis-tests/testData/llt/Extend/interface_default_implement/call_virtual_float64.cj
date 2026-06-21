

package test

public interface I {
    func bar(a: Int64): Unit {
        println("bar")
    }

    func bar(): Unit {
        println("bar empty")
    }
}

extend Float64 <: I {
    public func bar(): Unit {
        println("Float64 bar empty")
    }
}

main() {
    let f: Float64 = 3.2
    f.bar()
    f.bar(123)
}
