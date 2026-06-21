

enum E {
    | A
    | A(Int64)
}

main() {
    var a = 1
    if (let A <- A(1)) {
        a++
    } else {
        a--
    }
    return a
}
