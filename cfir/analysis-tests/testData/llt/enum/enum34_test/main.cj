// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f
// ASSERT: scan ADD 1 SUB 2 3struct

internal import pkg.*

main() {
    var e = Add(Ele(1), Sub(Ele(2), Ele(3)))
    print(e)
    let r = Rcd<Int64>()
    print(r)
    return 0
}
