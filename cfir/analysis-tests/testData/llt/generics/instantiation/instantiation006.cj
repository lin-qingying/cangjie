// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %enableO2 %f -o %output
// EXEC: %run %run_opt %output

class A<T> {}

interface Iaa<T> {
    static func foo(a: T) {
        return a
    }
    func foo1(a: T) {
        return a
    }
}

extend<T> A<T> <: Iaa<Int64> {}

extend<T> A<T> <: Iaa<Int32> {}

main() {
    var a1: Int32 = 1
    var a2: Int32 = -1
    return A<Int64>().foo1(a2) + A<Int64>.foo(a1)
}
