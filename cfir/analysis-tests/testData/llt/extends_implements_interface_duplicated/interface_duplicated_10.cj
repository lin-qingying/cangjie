// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

interface I<K> {}

open class A1<T> <: B<Int64> & I<Int64> {}

open class A2<T> <: B<T> & I<Int64> {}

open class B<T> <: I<T> {}

open class C<L> <: A2<L> & I<Int32> & I<Rune> {}

open class D<P> <: C<P> {}

open class E<U> <: D<Int32> {}

main() {
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
