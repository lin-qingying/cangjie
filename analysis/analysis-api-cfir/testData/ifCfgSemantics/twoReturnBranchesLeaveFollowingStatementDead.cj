package test.ifcfg.bothreturn
func mark(value: Int64): Unit {}
func tested(flag: Bool): Int64 {
    /*@guard*/if (flag) { /*@thenReturn*/return 11 } else { /*@elseReturn*/return 22 }
    /*@deadTail*/mark(99)
    0
}
