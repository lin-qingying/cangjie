


class M {
    let size = 3
}

class S {
    let x = M()
}

class C<T> {
    C(let value: T) {}
}

extend<T> C<T> where T <: S {
    func f() {
        let y = value.x.size
        return y
    }
}

main(): Int64 {
    let a = C<S>(S())
    if (a.f() != 3) {
        return 1
    }
    return 0
}
