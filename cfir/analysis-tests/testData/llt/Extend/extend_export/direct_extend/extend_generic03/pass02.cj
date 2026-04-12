// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC: %compiler %cmp_opt a.%middle %f -o %output
// EXEC-PIPE-0: %run %output

package pkg

import pkga.A as TTA

class B <: TTA {
    func foo_b() {
        foo() + foo1() + me()
    }
}

main(): Int64 {
    return B().me() + B().foo_b() - 6
}
