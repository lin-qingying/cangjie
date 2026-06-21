


enum TimeUnit {
    Year(Int32)
}

main() {
    let intArray: Array<TimeUnit> = [Year(1), Year(2)]
    var a: Int32 = 0
    for (Year(x) in intArray) { // enum pattern contain variable pattern
        a += x
    }
    if (a != 3) {
        return 1
    }
    return 0
}
