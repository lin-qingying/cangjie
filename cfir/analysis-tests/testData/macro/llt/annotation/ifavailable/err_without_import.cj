

main() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!>
}
