// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o libpkga.a
// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

// ASSERT: scan error: cannot assign to immutable value

import pkga.foo

class B {
    public static let CTX: Int64

    static init() {
        CTX = 1
        foo(
            {
                =>
                    CTX = 10
                    println(CTX)
            }
        )
    }
}

main() {}
