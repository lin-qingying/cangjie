

main() {
    let a = Sub()
    // Checking infered function ret type.
    let b: Sub = g(a)

    // Checking infered variable type.
    let ff = a.f
    let c: Sub = ff()
}

open class Base {
    protected open func f(): This {
        this
    }
}

class Sub <: Base {}

func g<T>(a: T) where T <: Base {
    let ff = a.f
    ff()
}
