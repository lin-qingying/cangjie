

open class Base {
    protected open func f(): This {
        this
    }
}

class Sub <: Base {
    func test(): Unit {
        let ff = f
        let c: Sub = ff()
    }
}

main() {}
