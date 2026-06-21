


foreign func c_f1(a: VArray<Int32, $3>): <!VARRAY_IN_CFUNC!>VArray<Int32, $3><!> // error

@C
func c_f2(a: VArray<Int32, $3>): <!VARRAY_IN_CFUNC!>VArray<Int32, $3><!> {
    a
} // error

var c_f3: CFunc<(VArray<Int32, $3>) -> <!VARRAY_IN_CFUNC!>VArray<Int32, $3><!>> = {a => a} //error

foreign func c_f4(a: CPointer<VArray<Int32, $1>>): CPointer<VArray<Int32, $1>> // error

@C
func c_f5(a: CPointer<VArray<Int32, $1>>): CPointer<VArray<Int32, $1>> {
    a
} // error

var c_f6: CFunc<(CPointer<VArray<Int32, $1>>) -> CPointer<VArray<Int32, $1>>> = {a => a} //error

main() {}
