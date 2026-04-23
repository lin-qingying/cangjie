// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {
    static func f1(a: T): Unit
    static func f2(a: T): Unit {
        f1(a)
    }
}

interface I2 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        println("Int64 f1 ${a}")
    }
    static func f1(a: String): Unit {
        println("String f1 ${a}")
    }
}

main() {
    I2.f2(123)
    I2.f2("321")
}
