package test.ifcfg.noelse
func mark(value: Int64): Unit {}
func tested(flag: Bool): Int64 {
    /*@guard*/if (flag) { /*@throw*/throw Exception("then") }
    /*@tail*/mark(1)
    41
}
