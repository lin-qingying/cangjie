// DEPENDENCE: pkga/pkga.cj



internal import pkga.*

func run(): Int64 {
    i--
    i++
    if (i == -2) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
