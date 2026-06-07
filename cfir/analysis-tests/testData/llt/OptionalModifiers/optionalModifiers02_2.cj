// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    // with default implementation
    func f(a: Int64) {
        a
    }
    static func g(a: Int64) {
        a
    }
}

interface I2 <: I1 {
    func f(a: Int32) {
        a + 1
    }
    static func g(a: Int64) {
        a + 1
    }
}

interface I3 <: I1 {
    <!NOTHING_TO_OVERRIDE!>override<!> func f(a: Int32) {
        a + 1
    }
    static <!NOTHING_TO_OVERRIDE!>redef<!> func g(a: Int32) {
        a
    }
}

interface I4 <: I1 {
    override func f(a: Int64) {
        a + 1
    }
}

main() {}
