

open class A {
    public static func f() {}
}

class B <: A {
    private static redef func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {
        print(1)
    }
}

main() {
}
