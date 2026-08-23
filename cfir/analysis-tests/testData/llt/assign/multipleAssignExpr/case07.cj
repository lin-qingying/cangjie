



interface I {}

class A {}

extend A <: I {}

func get(): ((A, A), Int64) {
    return ((A(), A()), 1)
}

main() {
    var i: (I, I)
    var val = 1
    (i, val) = get()
    0
}

