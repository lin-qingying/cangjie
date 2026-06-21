

interface Ser<T> {
    static func foo(): Unit
}

class AL<T> {}

extend<T> AL<T> <: Ser<AL<T>> where T <: Ser<T> {
    public static func foo(): Unit {}
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>DM<!> <: Ser<DM> {}

main() {
    var a = AL<DM>()
}
