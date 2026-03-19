// RUN_PIPELINE_TILL: FRONTEND
func choose(flag: Bool): Int32 {
    if (flag) {
        return 1
    } else {
        return 2
    }
}

main(): Int32 {
    return choose(true)
}


