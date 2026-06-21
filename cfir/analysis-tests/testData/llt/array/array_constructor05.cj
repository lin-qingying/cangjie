

import std.core
// Test for type priority.
class Array {
    var v = 1
}

main() {
    let a = Array()
    if (a.v != 1) {
        return 1
    }
    let arr0 = core.Array(3, repeat: 'c')
    let arr1 = core.Array<Int8>(3, repeat: 3)
    let arr2: core.Array<ToString> = core.Array(3, repeat: 3)
    if (arr0[2] != 'c' || arr1[2] != 3) {
        return 1
    }
    if (arr2[2].toString() != "3") {
        return 2
    }
    return 0
}
