// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output
// EXEC: %run %run_opt %output | compare %f
// (CJNATIVE)ASSERT: scan 2
// (not CJNATIVE)ASSERT: scan 1

import pkg.*

interface Real<T> <: I1<T> {
    func foo(): Unit {
        println(2)
    }
}

extend A <: Real<Int64> {}

main() {
    A().foo()
}
