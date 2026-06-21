// FILE: pkg/pkg.cj


package pkg

public open class A<T> {}

extend<T> A<T> <: Equatable<A<T>> {
    public operator func ==(ot: A<T>) {
        refEq(this, ot)
    }
    public operator func !=(ot: A<T>) {
        false
    }
}

public class B <: A<Int64> {}

extend B <: Equatable<B> {
    public operator func ==(ot: B) {
        refEq(this, ot)
    }
    public operator func !=(ot: B) {
        false
    }
}

// FILE: main.cj



import pkg.*

func bar(): Any {
    let a = B()
    return a
}

main() {
    var res = bar()
    if (res is B) {
        0
    } else {
        1
    }
}
