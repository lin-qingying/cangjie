


enum E {
    | A
    | A(Int64)
}

main() {
    var a = 1
    while (let A <- A(1)) {
        a++
        break
    }
    a--
    return a
}
