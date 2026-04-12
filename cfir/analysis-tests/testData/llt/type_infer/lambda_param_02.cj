// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// basic cases

let f2 = {x => let v: Int = x}
let f2_2 = {x => let v: Any = x}
let f3 = {
    x => if (x) {
        1
    } else {
        2
    }
}
let f3_2 = {
    x => if (!x) {
        1
    } else {
        2
    }
}

main() {
    println(f2(2))
    println(f3(true))
}
