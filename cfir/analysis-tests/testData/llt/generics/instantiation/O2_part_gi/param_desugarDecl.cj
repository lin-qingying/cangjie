// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f %enableO2 -o %output
// RUN-EXEC: %run %run_opt %output %run_args

class A<T> {
    public A(let b!: Int32 = 0) {
    }
}

interface I<T> {
    static func foo<T>() {
        1
    }
}

extend<T> A<T> <: I<T> {
    public static func me<T>(a!: () -> Int64 = foo<T>) {
        a()
    }
}

main(): Int64 {
    return A<Int32>.me<Int64>() - 1
}
