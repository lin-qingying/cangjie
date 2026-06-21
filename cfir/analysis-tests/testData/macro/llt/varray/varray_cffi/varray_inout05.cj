


let f1: CFunc<(VArray<Int8, $2>) -> Int8> = {arr => arr[0] + arr[1]}

@C
func f2(arr: VArray<Int8, $2>): Int8 {
    arr[0] + arr[1]
}

main() {
    var v1: VArray<Int8, $2> = [2, 3]
    var x = unsafe { f1(inout v1) }
    var y = unsafe { f2(inout v1) }
    if (x != 5 || y != 5) {
        return 1
    }
    return 0
}
