package test.ifcfg.finallyreturn
func tested(flag: Bool): Int64 {
    try { /*@oldReturn*/return 11 }
    finally { /*@guard*/if (flag) { /*@newReturn*/return 22 } }
}
