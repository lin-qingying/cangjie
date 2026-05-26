package sample.lowlevel.lazy.ref

func identity(value: Int64): Int64 {
    return value
}

func consume(): Int64 {
    return iden<caret>tity(1)
}
