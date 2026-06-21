


main() {
    var a = VArray<Int64, $3>(repeat: 0)
    <!NO_MATCHING_OPERATOR_INVOKE!>a<!>.size()
    return 0
}
