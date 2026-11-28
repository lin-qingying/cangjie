package test.ifcfg.branchlambda
func select<T>(body: () -> T): T { body() }
func tested(flag: Bool): Int64 {
    /*@selectCall*/select(/*@guard*/if (flag) {
        { => 11 }
    } else {
        { => 22 }
    })
}
