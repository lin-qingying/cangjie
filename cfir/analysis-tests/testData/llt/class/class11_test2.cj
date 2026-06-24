


interface A {
    func i() {}
    static func test(<!CANNOT_HAVE_DEFAULT_PARAM!>input!: Unit = <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>.i()<!>) {
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>.i()
    }
}

main(): Int64 {
    return 1
}
