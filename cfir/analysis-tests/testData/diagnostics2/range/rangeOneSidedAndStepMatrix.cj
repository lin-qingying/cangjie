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
    let broken = 0..10 : <!TYPE_MISMATCH!>false<!>
    let _ = broken
}

func boundedRangeBadFloatStep(): Unit {
    let broken = 10..0 : <!TYPE_MISMATCH!>1.0<!>
    let _ = broken
}
