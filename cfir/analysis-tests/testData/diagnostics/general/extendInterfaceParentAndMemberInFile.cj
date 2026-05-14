interface A{
    func a(): Int
}

class B{
}

extend B <: A{
    public func a(): Int {
        return 1
    }
}

main(): Unit {
    let interfaceValue: A = B()
    let classValue = B()

    interfaceValue.a()
    classValue.a()
}
