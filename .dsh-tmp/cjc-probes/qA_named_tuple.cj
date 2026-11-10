type NamedTuple = (x: Int64, y: String)

func useIt(t: NamedTuple): Int64 {
    return t.x
}

func useIdx(t: NamedTuple): String {
    return t[1]
}
