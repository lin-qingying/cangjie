

open class C1 {
    public open func foo(): C1 {
        return this
    }
}

class C2 <: C1 {
    public override func foo(): This {
        return this
    }
}

main() {
    return 0
}
