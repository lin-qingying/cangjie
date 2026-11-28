package test.ifcfg.branchcalls
func retain<T>(body: () -> T): () -> T { body }
func consume<T>(body: () -> T): T { body() }
func tested(flag: Bool): Int64 {
    /*@consumeCall*/consume(/*@guard*/if (flag) {
        /*@leftCarrier*/retain({ => 11 })
    } else {
        /*@rightCarrier*/retain({ => 22 })
    })
}
