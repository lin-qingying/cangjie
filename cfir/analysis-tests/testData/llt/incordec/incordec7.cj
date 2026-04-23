// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let a = B()
    return 0
}

open class A {
    public var p: Int64 = 8
}

class B <: A {
    public var a: Unit = ()
    public func foo() {
        this.p++
        p++
        super.p++
        <!TYPE_MISMATCH!>this.a++<!>
        <!TYPE_MISMATCH!>a++<!>
        a
    }
}
