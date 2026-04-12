// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan foo

interface I<T> {
    func foo(a: T): Unit {
        println("foo")
    }
}

interface I1<T> <: I<T> {}

class A<T> {}

extend<T> A<T> <: I<T> {}

extend<T> A<T> <: I1<T> {}

main() {
    A<Int64>().foo(1)
}
