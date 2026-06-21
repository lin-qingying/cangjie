// FILE: pkg.cj


package pkg

public enum Expr<T> <: ToString where T <: ToString {
    | Ele(T)
    | Add(Expr<T>, Expr<T>)
    | Sub(Expr<T>, Expr<T>)

    public func toString(): String {
        match (this) {
            case Ele(e) => e.toString()
            case Add(e1, e2) => "ADD ${e1} ${e2}"
            case Sub(e1, e2) => "SUB ${e1} ${e2}"
        }
    }
}

public struct Rcd<T> <: ToString where T <: ToString {
    public func toString(): String {
        "struct"
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    var e = Add(Ele(1), Sub(Ele(2), Ele(3)))
    print(e)
    let r = Rcd<Int64>()
    print(r)
    return 0
}
