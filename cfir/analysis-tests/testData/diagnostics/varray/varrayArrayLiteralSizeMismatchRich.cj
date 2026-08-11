// RUN_PIPELINE_TILL: FRONTEND
type TwoNums = VArray<Int64, $2>

func acceptTwo(values: TwoNums) {
    let _ = values
}

func argumentMismatch() {
    acceptTwo(<!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!>)
}

func initializerMismatch(current: TwoNums) {
    let copy: TwoNums = <!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!>
    var target: TwoNums = current
    target = <!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!>
}

func returnMismatch(): TwoNums {
    return <!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!>
}
