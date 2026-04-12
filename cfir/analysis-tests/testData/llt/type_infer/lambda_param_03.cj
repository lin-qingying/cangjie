// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// overloading and generic func

func same<T>(x: T, y: T) where T <: Equal<T> {
    x == y
}

let f4 = {x => same(1, x)}

func g1(x: Int, y: Int) {
    x
}

func g1(x: String, y: String) {
    y
}

func g2(x: Int) {
    x
}

func g2(x: String) {
    x
}

let f7 = {x, y => g1(x, "123") == g1("456", y)}
let f8 = {x => let v: Int = g2(x)}

main() {
    println(f4(4))
    println(f7("7", "7"))
    println(f8(8))
}
