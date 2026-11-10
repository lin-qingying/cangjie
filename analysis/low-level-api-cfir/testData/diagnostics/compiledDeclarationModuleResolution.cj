package sample.compiledmodule

func useCompiledArray(): Int64 {
    let values: Array<Int64> = Array<Int64>(3, repeat: 0)
    return values.size
}
