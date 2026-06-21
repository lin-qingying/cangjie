

main(): Int64 {
    return 0
}

func case03<T>() {
    1
}

func case04() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>case03<!>
}
