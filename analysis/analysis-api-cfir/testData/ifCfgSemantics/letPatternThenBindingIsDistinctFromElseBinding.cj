package test.ifcfg.binding
func mark(value: Int64): Unit {}
func tested(value: ?Int64): Int64 {
    /*@guard*/if (let Some(item) <- value) {
        /*@thenCall*/mark(/*@thenUse*/item)
        return item
    } else {
        let item: Int64 = 7
        /*@elseCall*/mark(/*@elseUse*/item)
        return item
    }
}
