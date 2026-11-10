type NoArg = () -> Unit

func call(g: NoArg): Unit {
    return g()
}
