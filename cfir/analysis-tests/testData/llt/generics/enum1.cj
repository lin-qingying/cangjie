

enum TimeUnit2<T> {
    Year2
}

enum TimeUnit {
    Year2(<!UNRESOLVED_REFERENCE!>T<!>)
}

main() {
    var time = TimeUnit2<Int32>.Year2
    TimeUnit2<Float64>.Year2
    TimeUnit2<Float64>.Year2
    <!GENERIC_ARGUMENT_NO_MATCH!>TimeUnit2<Float64, Int32>.Year2<!>
    TimeUnit2<Int32>.Year2
    <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>TimeUnit2<!>.Year2
}
