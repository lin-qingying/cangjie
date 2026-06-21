


main() {
    let a = None<Int64>
    if (let <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Option<Int64>.None(v)<!> <- a) {
        return 1
    }
    return 0
}
