// RUN_PIPELINE_TILL: FRONTEND
foreign func swap(value: Int64, other: Int64): Unit

struct Box {
    var value: Int64 = 0
}

func propertyInout(box: Box): Unit {
    unsafe {
        swap(inout <!INOUT_MUST_BE_VAR_VARIABLE!>box<!>.value, 1)
    }
}

func duplicateInout(): Unit {
    var x = 1
    unsafe {
        swap(inout x, inout x)
    }
}
