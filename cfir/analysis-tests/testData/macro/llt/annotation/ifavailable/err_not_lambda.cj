

func mem() {}

main() {
    @IfAvailable(APILevel:11, <!IFAVAILABLE_ARG_NOT_LITERAL!>mem<!>, <!IFAVAILABLE_ARG_NOT_LITERAL!>mem<!>)
}
