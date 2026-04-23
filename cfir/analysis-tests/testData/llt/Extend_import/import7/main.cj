// DEPENDENCE: A.cj B.cj C.cj

internal import A.Foo
internal import B.I1
internal import C.I2

main(): Int64 {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 1
}
