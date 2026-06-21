

main() {
    var a = 10
    var _ = <!CONST_EVAL_DIVIDE_BY_ZERO!>a % 0<!>
    var _ = <!CONST_EVAL_DIVIDE_BY_ZERO!>a / 0<!>
    return 0
}
