

import std.collection.*

let array = [1, 2, 3]
let list = ArrayList<Int64>()

main() {
    test(100)
    println(array)
    println(list)
}

const func test(a: Int64) {
    <!EXPECT_CONST!>array<!>[0]++
    <!EXPECT_CONST!>array<!>[1] = a
    <!EXPECT_CONST!>list<!>.add(a)
}
