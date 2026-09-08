

main(): Int64 {
    return 0
}

func case03<T>() {
    1
}

func case04() {
    <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>case03<!>
}
