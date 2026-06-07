// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    // no default implementation
    func f(a: Int64): Int32
    static func sf(a: Int64): Int32
}

interface I2 <: I1 {
    override func f(a: Int64) {
        Int32(a + 1)
    }
    static redef func sf(a: Int64): Int32
}

interface I3 <: I1 {
    <!NOTHING_TO_OVERRIDE!>override<!> func f(a: Int32) {
        a + 1
    }
    static <!NOTHING_TO_OVERRIDE!>redef<!> func sf(a: Int32): Int32
}

interface I4 <: I1 {
    <!NOTHING_TO_OVERRIDE!>override<!> func f(a: Int32): Int32
}

main() {}
