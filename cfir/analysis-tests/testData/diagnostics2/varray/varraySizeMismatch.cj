// RUN_PIPELINE_TILL: FRONTEND
type TwoNums = VArray<Int64, $2>
type ThreeNums = VArray<Int64, $3>

func acceptTwo(values: TwoNums) {}

func keepTwo(values: TwoNums): TwoNums {
    return values
}

func argumentMismatch(values: ThreeNums) {
    acceptTwo(<!VARRAY_SIZE_MISMATCH!>values<!>)
}

func initializerMismatch(values: ThreeNums, current: TwoNums) {
    let copy: TwoNums = <!VARRAY_SIZE_MISMATCH!>values<!>
    var target: TwoNums = current
    target = <!VARRAY_SIZE_MISMATCH!>values<!>
}

func returnMismatch(values: ThreeNums): TwoNums {
    return <!VARRAY_SIZE_MISMATCH!>values<!>
}

func ok(values: TwoNums) {
    acceptTwo(values)
    let copy: TwoNums = values
    var target: TwoNums = keepTwo(values)
    target = values
}
