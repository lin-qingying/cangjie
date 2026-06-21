


open class A {
    public open func test(<!CANNOT_HAVE_DEFAULT_PARAM!>a!: Int64 = 1<!>) {
        0
    }
}
