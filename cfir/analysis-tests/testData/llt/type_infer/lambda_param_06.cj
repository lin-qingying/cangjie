// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// solve by generic constraint, and type var created on-the-fly

let f12 = {x => "this is ${x}"}
let f15 = {x => "this is ${Some(x)}"}
let f16 = {g => "this is ${g(0)}"}

main() {
    println(f12(12))
    println(f15(15))
    println(f16({x => x + 16}))
}
