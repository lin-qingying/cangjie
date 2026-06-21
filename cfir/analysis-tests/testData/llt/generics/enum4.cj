

enum TimeUnit<T> {
    Day | Year(T)
}

main() {
    var time = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Day<!>
    var time1 = Year(1)
    return 1
}
