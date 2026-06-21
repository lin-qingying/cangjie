// FILE: pkg.cj


package pkg

public enum Op<T> {
    Val(T) | None
}

extend<T> Op<T> {
    func isNone(): Bool {
        match (this) {
            case Val(v) => false
            case None => true
        }
    }
}

extend<T> Op<T> where T <: Equatable<T> {
    operator func ==(right: Op<T>): Bool {
        match (this) {
            case Val(v) => match (right) {
                case Val(r) => (r == v)
                case None => false
            }
            case None => match (right) {
                case Val(r) => false
                case None => true
            }
        }
    }

    operator func !=(right: Op<T>): Bool {
        !(this == right)
    }
}

public func test() {
    let a = Val(1)
    let b = Val('a')
}

// FILE: main.cj



import pkg.*

main(): Int64 {
    let a = Val(1)
    0
}
