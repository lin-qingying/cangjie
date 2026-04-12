// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A<T> {
    func test(a: T): T {
        return a
    }
}

interface B<T> <: A<T> {}

class C<T> <: B<T> {}

main(): Int64 {
    var ans1 = C<Int32>()
    if (ans1.test(1) == 1) {
        return 0;
    }
    return 1
}
