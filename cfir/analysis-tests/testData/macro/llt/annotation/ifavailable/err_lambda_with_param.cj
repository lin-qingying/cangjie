

main() {
    @IfAvailable(<!IFAVAILABLE_UNKNOWN_ARG_NAME!>Level<!>:11, {=>}, <!TYPE_MISMATCH!>{ a:Int=>() }<!>)
}
