


main() {
    let a1_1 = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>(repeat: 1, {i => i})<!> // x2 error
    let a1_2 = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>()<!> // x2 error
    let a1_3: VArray<Int64, $3> = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>()<!> // error
    let a1_4 = VArray<<!UNDECLARED_TYPE_NAME!>C<!>, $3>(repeat: 0) // error
    0
}
