main() {
    let a = None<Int64>
    if (let Option<Int64>.None(v) <- a) {
        return 1
    }
    return 0
}
