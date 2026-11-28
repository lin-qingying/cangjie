package sample.scope

func tracked(): Int64 {
    1
}

func consume(value: Int64): Int64 {
    let localValue = value
    let shadowed = localValue
    tracked()
    return shadowed
}

main() {
    consume(tracked())
}