package test.ifcfg.finallyflow
func mark(value: Int64): Unit {}
func tested(flag: Bool): Int64 {
    try {
        /*@guard*/if (flag) { /*@throw*/throw Exception("then") }
        /*@normal*/mark(1)
    } catch (e: Exception) {
        mark(2)
    } finally {
        mark(3)
    }
    /*@tail*/mark(4)
    41
}
