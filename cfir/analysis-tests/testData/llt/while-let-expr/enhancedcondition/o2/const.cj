

main() {
    let a = 3
    var res = 0
    while (let 2 | 3 | 4 <- a) {
        res++;
        break
    }
    while (let 0 | 2 | -1 <- a) {
        res--;
        break
    }
    res
}
