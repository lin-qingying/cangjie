// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Foo <: Bar {
    static let y: Int64
    static init() { // called first
        z
        y = <!USED_BEFORE_INITIALIZATION!>x<!> // error: not yet initialized variable
    }
    static let z = 1
}

open class Bar {
    static let x: Int64
    static init() { // called second
        x = 2
    }
}

var b = Bar.x

main() {
    println(Foo.y)
    println(Bar.x)
}
