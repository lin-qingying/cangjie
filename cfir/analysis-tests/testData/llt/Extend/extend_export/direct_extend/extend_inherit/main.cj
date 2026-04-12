// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1.cj, p2.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt p1.cj -o p1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt p2.cj -o p2.%middle
// EXEC: %compiler %cmp_opt p1.%middle p2.%middle %f -o %output
// EXEC-PIPE-0: %run %output

import p2.B

class C <: B {
    func foo2() {
        foo()
    }
}

main() {
    0
}
