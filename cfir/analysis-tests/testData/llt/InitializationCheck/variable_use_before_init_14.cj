

main() {
    var (a, b): (Int64, Int64)
    println(<!USED_BEFORE_INITIALIZATION!>a<!>)
    println(<!USED_BEFORE_INITIALIZATION!>b<!>)
}
