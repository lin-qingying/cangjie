

abstract class A {
    public func f(<!CANNOT_HAVE_DEFAULT_PARAM!>x!: Bool = true<!>): Unit
}

main() {}
