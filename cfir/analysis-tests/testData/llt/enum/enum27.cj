// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum C {
    c

    public func test(a: Int64) {
        a
    }
}

func bar(x: Int64) {
    1
}

main(): Int64 {
    var x = C.c
    let c1 = Option<C>.Some(x)
    let r1 = c1?.test
    return (r1 ?? bar)(0)
}
