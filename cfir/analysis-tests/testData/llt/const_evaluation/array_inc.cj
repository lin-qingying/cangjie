// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
