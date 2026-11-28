package test.ifcfg.elseifchain
func mark(value: Int64): Unit {}
func tested(first: Bool, second: Bool): Int64 {
    /*@outerIf*/if (first) { /*@outerThrow*/throw Exception("first") }
    else /*@innerIf*/if (second) { /*@innerThrow*/throw Exception("second") }
    /*@tail*/mark(1)
    41
}
