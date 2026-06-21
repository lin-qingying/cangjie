// FILE: pkg.cj


package pkg

public interface I<T> where T <: I<T> {
    operator func -(): T
    operator func <<(other: T): T
}

extend Int64 <: I<Int64> {}

// FILE: ok.cj


internal import pkg.I

func f<T>() {
    var x = 0
    x <<= 1
}

main() {}
