// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Base {}

class Sub <: Base {
    public let f = {x: Sub => 3}
    public func ff(x: Sub, y!: Int32 = 0): Int32 {
        4
    }
    public func ff(x: Sub): Int32 {
        5
    }
}

func a(x: Base) {
    return Base()
}

func a(x: Sub): Int32 {
    return 2
}

func a(x: Int32, y: Int32): Int32 {
    return x + y
}

func codegenUnitTest(): Int64 {
    var x: (Int64) -> Int32 = a
    var b = <!AMBIGUOUS_USE!>Sub().ff<!>
    var z = <!AMBIGUOUS_USE!>a<!>
    return 0
}

main(): Int64 {
    return codegenUnitTest()
}
