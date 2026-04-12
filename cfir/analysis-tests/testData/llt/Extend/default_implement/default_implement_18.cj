// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// (CJNATIVE)ASSERT: scan 2
// (not CJNATIVE)ASSERT: scan 1

interface I1 <: I<Int64> {
    func foo(a: Int64): Unit {
        println(2)
    }
}

interface I<T> {
    func foo(a: T): Unit {
        println(1)
    }
}

class A {}

extend A <: I<Int64> {}

extend A <: I1 {}

main() {
    A().foo(1)
}
