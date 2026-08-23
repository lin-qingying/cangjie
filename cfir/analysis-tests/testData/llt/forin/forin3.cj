



func case01() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    let b: Int32 = 2
    for (2 in intList) {
        a = b
    }
    a
}

func case03() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    for (_: Int32 in intList) {
        a++
    }
    a
}

enum TimeUnit {
    Year(Int32)
    | Mounth
}

main() {
    let intList: Array<TimeUnit>
    let a: Array<TimeUnit> = [Year(1), Year(2)]
    for (Year(x) in a) {
    }
    0
}

