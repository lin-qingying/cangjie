// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    protected static var a1: Int64 = 1
    private static var a2: Int64 = 2
    private <!parse_unexpected_declaration_in_scope!>class<!> A1 {
        public var a3 = a1
        public var a4 = a2
    }
    protected <!parse_unexpected_declaration_in_scope!>interface<!> I1{}
}

class B <: A {
    public var b: A1 = A1()
    public <!parse_unexpected_declaration_in_scope!>class<!> B1 {
        public <!parse_unexpected_declaration_in_scope!>class<!> B2 {
            public var b1 = a1
            public var b2 = a2
            public var b3 : I1
        }
    }
}
