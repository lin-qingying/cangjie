package sample.dangling

func tracked(): Int64 {
    return 1
}

func consume(): Int64 {
    let localValue = 2
    let produced = tracked()
    return localValue + produced
}
