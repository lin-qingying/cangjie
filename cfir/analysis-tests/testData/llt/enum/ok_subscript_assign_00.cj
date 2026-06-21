

enum E {
    Arr(Array<Int64>)

    operator func [](idx: Int64, value!: Int64) {
        match (this) {
            case Arr(arr) => arr[idx] = value
        }
    }
}

main() {
    let e = Arr([1, 2, 3])
    e[0] = 0
    match (e) {
        case Arr(arr) => println(arr)
    }
}
