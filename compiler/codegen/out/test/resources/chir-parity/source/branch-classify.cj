// RUN_PIPELINE_TILL: FRONTEND
func classify(x: Int64): Int64 {
    if (x > 0) {
        return 1
    } else if (x < 0) {
        return -1
    }
    return 0
}

main(): Int64 {
    return classify(-8)
}

