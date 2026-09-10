class First {}
class Second {}

func earlier(value: First): Int64 {
    match (value) {
        case _: Second => 0
        case _ => 1
    }
}

func later() {
    <!UNRESOLVED_REFERENCE!>missingValue<!>
}
