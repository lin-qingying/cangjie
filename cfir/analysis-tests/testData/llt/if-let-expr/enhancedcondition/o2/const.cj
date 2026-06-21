

main() {
    let a = 3
    var res = 0
    if (let 2 | 3 | 4 <- a) {
        res++
    }
    if (let 0 | 2 | -1 <- a) {
    } else {
        res++
    }
    res
}
