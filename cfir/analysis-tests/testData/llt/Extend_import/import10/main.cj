// FILE: pkg.cj


package pkg

public interface I {
    func f(): Int64
}

extend Int64 <: I {
    public func f(): Int64 {
        return 111
    }
}

public func fff(x: Array<I>) {
    x[0].f()
}

public func ggg(x: I) {
    x.f()
}

// FILE: main.cj



internal import pkg.*

main(): Int64 {
    let a = fff([1])
    if (a != 111) {
        return 1
    }
    return 0
}
