interface A{
    func a(): Int
}

class B{
}

extend B <: A{
    func a(): Int {
        return 1
    }
}

main() {
    let interfaceValue: A = B()
    let classValue = B()

    interfaceValue.a()
    classValue.a()
}
