// RUN_PIPELINE_TILL: FRONTEND
type TwoNums = VArray<Int64, $2>
type ThreeNums = VArray<Int64, $3>

func acceptTwo(values: TwoNums) {
    let _ = values
}

func keepTwo(values: TwoNums): TwoNums {
    return values
}

func argumentMismatch(values: ThreeNums) {
    acceptTwo(<!NEW_INFERENCE_ERROR!>values<!>)
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
    keepTwo(values)
}
