// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var a:Int64 =3

}

extend A {
   public static func g():Int64 {
        func f() {
            a
            0
        }
        0
    }
}

class B{
    public var a:Int64 =3

}

extend B {
    protected prop f: String {
        get() {
            return "C"
        }
    }
       public <!parse_illegal_modifier_in_scope!>open<!> prop f1: String {
        get() {
            return "C"
        }
    }
}
