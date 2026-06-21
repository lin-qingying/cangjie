


open class C {
    operator func ()(x: Int) {
        x
    }
}

class C2 <: C {}

func foo<T>(f: T) where T <: C {
    f(0)
}

main() {
    foo(C2())
}
