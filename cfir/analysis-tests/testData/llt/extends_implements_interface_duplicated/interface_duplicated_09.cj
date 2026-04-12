// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

interface I0<T> {}

interface I1<T> <: I0<T> {}

interface I2<T> <: I1<T> {}

open class A<T> <: I2<T> {}

open class B<P> <: A<P> {}

class D<T> <: A<T> & I0<Int32> & I0<Int64> {}

main() {
    var b = B<Int64>()
    var d1 = D<Int64>()
    var d2 = D<Int32>()
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
