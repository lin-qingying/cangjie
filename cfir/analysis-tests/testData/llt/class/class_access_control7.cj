// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public var a: Int32 = 1
    private var b: Int32 = a
    protected var d: Int32 = 2
    public var c: Int32 = d
}

class B <: A {
    private var e = <!USED_BEFORE_INITIALIZATION!>a<!>
    private var g = <!USED_BEFORE_INITIALIZATION!>d<!>
    private var h = <!USED_BEFORE_INITIALIZATION!>c<!>

    public var i = <!USED_BEFORE_INITIALIZATION!>a<!>
    public var k = <!USED_BEFORE_INITIALIZATION!>d<!>
    public var l = <!USED_BEFORE_INITIALIZATION!>c<!>

    protected var m = <!USED_BEFORE_INITIALIZATION!>a<!>
    protected var o = <!USED_BEFORE_INITIALIZATION!>d<!>
    protected var p = <!USED_BEFORE_INITIALIZATION!>c<!>

    public func f() {
        if (e == 1 && g == 2 && h == 2 && i == 1 && k == 2 && l == 2 && m == 1 && o == 2 && p == 2) {
            return 0
        } else {
            return 1
        }
    }
}

main(): Int64 {
    var testB = B()
    testB.f()
    return 0
}
