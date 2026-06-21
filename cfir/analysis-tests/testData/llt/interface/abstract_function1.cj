

interface I {
    func f1(a!: Int64): Unit
    static func f2(<!CANNOT_HAVE_DEFAULT_PARAM!>a!: Int64 = 1<!>, b!: Int64): Unit
}
