

enum Time {
    Day(Int32)
}

main(): Int64 {
    var time = Day(3)
    var res: Int32 = match (time) {
        case Day(x) =>
            x
            return 0
    }
    return 1
}
