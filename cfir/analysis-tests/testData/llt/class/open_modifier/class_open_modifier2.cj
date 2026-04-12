// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    public open func f(): Int32 {
        return 0
    }

    public init() {
    }
}

class B <: A {
    public init() {
        f()
        var a = {=> f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
}

main(): Int64 {
    0
}
