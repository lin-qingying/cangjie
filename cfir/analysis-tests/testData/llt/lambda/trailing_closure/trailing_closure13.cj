

main(): Int64 {
    var x = {t: Int64 => 1}
    x <!TYPE_MISMATCH!>{}<!>
    x <!TYPE_MISMATCH!>{}<!>
}
