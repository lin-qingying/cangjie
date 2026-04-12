// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func foo(a: T): T {
        return a
    }
}

interface I2 <: I<Bool> {
    func bar(a: Int32): Int32
}

class B <: I2 {
    public func bar(a: Int32): Int32 {
        this.foo(false)
        return 0
    }
}

main(): Int64 {
    var a: I2 = B()
    return 0
}
