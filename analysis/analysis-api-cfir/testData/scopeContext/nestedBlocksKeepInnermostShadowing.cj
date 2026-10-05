package sample.scope

func nested(value: Int64): Int64 {
    let shadowed = value
    if (value > 0) {
        let shadowed = value + 1
        innerTarget
    }
    outerTarget
}

main() {
    nested(1)
}