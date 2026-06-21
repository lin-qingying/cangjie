

const v = 11

main() {
    @IfAvailable(level:<!IFAVAILABLE_ARG_NOT_LITERAL!>v<!>, { => println(0) }, {=>println(3)})
}
