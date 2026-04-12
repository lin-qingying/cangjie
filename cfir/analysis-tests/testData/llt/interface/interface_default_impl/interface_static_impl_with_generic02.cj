// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan String f1 123

interface I<T> {
    static func f1(a: T): Unit
}

interface I1<T> <: I<String> {
    static func f3(a: T) {
        f1("123")
    }
}

interface I2 <: I1<Int64> & I1<String> {
    static func f1(a: Int64): Unit {
        println("Int64 f1 ${a}")
    }
    static func f1(a: String): Unit {
        println("String f1 ${a}")
    }
}

main() {
    I2.f3(1)
}
