// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// generic container

import std.collection.*

let f11 = {
    x =>
    let arr = ArrayList<Int>()
    for (v in x) {
        arr.add(v + v)
    }
    arr
}
let f17 = {
    x => x |> map {x => x + 1} |> collectArray
}
let f26 = {
    l =>
    let i = l.iterator()
    var max = 0
    while (let Some(v) <- i.next()) {
        if (v > max) {
            max = v
        }
    }
    max
}

main() {
    println(f11([11, 12, 13]))
    println(f17([17, 18, 19]))
    println(f26([1, 26, 2, 3]))
}
