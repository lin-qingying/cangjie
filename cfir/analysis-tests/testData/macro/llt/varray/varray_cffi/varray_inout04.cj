


let f1: CFunc<(CPointer<Int64>, Int64) -> Unit> = {
    x, size =>
    var i = 0;
    while (i < size) {
        unsafe { x.write(i, x.read(i) + i) }
        i++
    }
}

main() {
    var v1: VArray<Int64, $3> = [1, 2, 3]
    unsafe { f1(inout v1, 3) }
    if (1 == v1[0] && 3 == v1[1] && 5 == v1[2]) {
        return 0
    }
    return 1
}
