// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Boo {
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var a: Int32 = 1 // a = 1
    public func f(): Int32 {
        m = 2
        0
    }
}

class Doo <: Boo {
    public var b: Int32 = <!USED_BEFORE_INITIALIZATION!>n<!>
    public var c: Unit = (<!USED_BEFORE_INITIALIZATION!>m<!> = 1)
    public var d: Int32 = <!USED_BEFORE_INITIALIZATION!>n<!>
    public var e: Int32 = <!USED_BEFORE_INITIALIZATION!>m<!>
    public var e1: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.m
    public func f1(): Int32 {
        m = 2
        0
    }
}
