func constEvalDivideByZero(): Int64 {
    let a: Int64 = <!CONST_EVAL_DIVIDE_BY_ZERO!>10 / 0<!>
    let b: Int64 = <!CONST_EVAL_DIVIDE_BY_ZERO!>10 % 0<!>
    return a + b
}
