

public open class A {
    public static func foo<T>(a: Array<T>): Unit {}
}

public open class B <: A {
    public static redef func foo<T>(a: Array<T>): Unit {}
}

main() {
    return 0
}
