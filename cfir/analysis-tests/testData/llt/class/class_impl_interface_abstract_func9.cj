// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

open class Father {}

class Child <: Father {}

interface I1 {
    func f(a: Int32): Father
}

interface I2 {
    func f(a: Int32): Child
}

class C <: I1 & I2 {
    public func f(a: Int32): Child {
        Child()
    }
}
