// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1 {}

interface I2 {}

class C <: I1 & I2 {}

let f1 = {_: I1 => ()}
let f2 = {_: I2 => ()}
let f3 = {_: C => ()}

main() {
    let f = match (1) {
        case 1 => f2
        case 2 => f1
        case 3 => f2
        case 4 => f1
        case 5 => f3
        case 6 => f1
        case 7 => f2
        case 8 => f1
        case _ => f2
    }
    f(C())
}
