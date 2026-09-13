

func mem() {}

main() {
    @IfAvailable(<!IFAVAILABLE_UNKNOWN_ARG_NAME!>APILevel<!>:11, <!IFAVAILABLE_ARG_NOT_LITERAL!>mem<!>, <!IFAVAILABLE_ARG_NOT_LITERAL!>mem<!>)
}
