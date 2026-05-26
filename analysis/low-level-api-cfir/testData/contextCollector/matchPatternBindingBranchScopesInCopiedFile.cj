package sample.lowlevel.contextcollector

enum TimeUnit {
    | Year(Int32)
    | Month(Int32, Int32)
}

func extractYear(time: TimeUnit): Int32 {
    return match (time) {
        case Year(y) => y
        case Month(y, _) => y
    }
}
