// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f
// ASSERT: scan 2

interface I1 {
    func foo(): Unit
}

open class A<T> {}

extend<T> A<T> {
    public func foo(): Unit {
        println(2)
    }
}

class B<T> <: A<T> & I1 {} // ok

main() {
    B<Int64>().foo()
    0
}
