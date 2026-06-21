

main() {
    let a: VArray<Int64, $3> = [1, 2, <!TYPE_MISMATCH!>"3"<!>] //error
    let a1: VArray<Int64, $1> = <!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!> //error
    0
}
