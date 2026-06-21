

// This for type check compilation
main() {
    let a = Sub()
    let b: Sub = g(a)
}

open class Base {
    protected open func f(): This {
        this
    }
}

class Sub <: Base {}

func g<T>(a: T) where T <: Base {
    a.f()
}
