// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var g: Int64 = 0

open class C {}

extend C {
    mut prop p: Int64 {
        get() {
            g
        }
        set(v) {
            g++
        }
    }
}

class C1 <: C {}

main() {
    let a = C()
    a.p = 0
    if (a.p != 1) {
        return 1
    }
    let b = C1()
    b.p = 0
    if (b.p != 2) {
        return 2
    }
    return 0
}
