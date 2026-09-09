// RUN_PIPELINE_TILL: FRONTEND
func boundedRangeWithPositiveStep(): Unit {
    let ok = 0..10 : 1
    let _ = ok
}

func boundedRangeWithNegativeStep(): Unit {
    let ok = 10..0 : -1
    let _ = ok
}

func boundedRangeBadBoolStep(): Unit {
    let broken = 0..10 : <!CANNOT_CONVERT_LITERAL!>false<!>
    let _ = broken
}

func boundedRangeBadFloatStep(): Unit {
    let broken = 10..0 : <!CANNOT_CONVERT_LITERAL!>1.0<!>
    let _ = broken
}
