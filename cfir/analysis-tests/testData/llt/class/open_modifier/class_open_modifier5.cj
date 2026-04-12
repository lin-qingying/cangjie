// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    public open operator func ()() {
        1
    }
}

class B <: A {
    public static var obj = A()
    public init() {
        obj()
    }
    public operator override func ()(): Int64 {
        2
    }
}

main(): Int64 {
    0
}
