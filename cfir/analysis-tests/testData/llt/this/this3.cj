// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

interface I {}

open class Base <: I {
    public static func f1(): Unit {
        println("Base f1")
    }
    public static func g1(): Unit {
        f1()
    }
}

class Sub <: Base {
    public static func f1(): Unit {
        println("Sub f1")
    }
}

main() {
    Sub.g1()
}

/* SCAN
Sub f1
*/
