// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %output | compare %f
// (CJNATIVE) ASSERT: scan-2 2
// (not CJNATIVE) ASSERT: scan-2 1

interface I1<T> {
    func foo() {
        1
    }
}

interface I2<U> <: I1<U> {
    func foo() {
        2
    }
}

class A<V> {}

extend<W> A<W> <: I2<W> {}

extend<X> A<X> <: I1<X> {}

class B<S> {}
// Diff order.
extend<Y> B<Y> <: I1<Y> {}

extend<Z> B<Z> <: I2<Z> {}

main() {
    println(A<Int32>().foo())
    println(B<Int32>().foo())
}
