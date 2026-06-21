

open class A {
    static func f<T>() {}
}

class B <: A {
    private static func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!><T>() {}
}

main() {}
