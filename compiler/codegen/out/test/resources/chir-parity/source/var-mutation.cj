// RUN_PIPELINE_TILL: FRONTEND
func mutate(x: Int64): Int64 {
    var y = x
    y = y + 1
    y = y * 2
    return y
}

main(): Int64 {
    return mutate(21)
}

