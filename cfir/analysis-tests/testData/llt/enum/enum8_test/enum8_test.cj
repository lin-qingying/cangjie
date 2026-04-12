// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC:%compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg/pkg.%middle
// EXEC:%compiler %cmp_opt pkg/pkg.%middle %n.cj --import-path pkg -o %output
// EXEC:%run %run_opt %output %run_args

internal import pkg.*

enum A {
    a
}

main() {
    let b: Unit1
    a
    b = Unit1.u1_3
    a
    let c: Unit1
    a
    c = Unit1.u1_5
    a

    let ret1 = match (b) {
        case u1_3 => 1
        case _ => 11
    }
    let ret2 = match (c) {
        case u1_5 => 2
        case _ => 22
    }
    if (ret1 == 1 && ret2 == 2) {
        return 0
    }
    return 1
}
