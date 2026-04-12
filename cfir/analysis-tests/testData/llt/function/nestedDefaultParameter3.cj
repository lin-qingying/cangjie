// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func test(): Int64 {
    let a: Int64 = 1
    func abc(): Int64 {
        a
    }
    func dfg(a: () -> Int64): Int64 {
        a()
    }
    dfg(abc)
}

main(): Int64 {
    func foo(a!: Int64 = (1, 2)[0]): Int64 {
        1
    }
    if (test() != 1) {
        return 1
    }
    return 0;
}
