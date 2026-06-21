

open class C1<T> {
    public open func foo(): This {
        return this
    }
}

class C2<T> <: C1<T> {
    public override func foo(): This {
        return this
    }
}

main() {
    return 0
}
