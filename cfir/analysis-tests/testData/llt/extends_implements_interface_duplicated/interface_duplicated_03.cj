// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

class A<Q> <: I1<Q> & I1<Int64> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    var k = A<Int32>()
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
