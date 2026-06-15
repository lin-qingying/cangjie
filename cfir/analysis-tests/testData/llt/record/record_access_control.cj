// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    public let a: Int64 = 1
    private let b: Int64 = 2
    public static let c: Int64 = 3
    private static let d: Int64 = 4
    public func m() {
        a // ok
        b // ok
        c // ok
        d // ok
        l() // ok
        o() // ok
    }
    private func l() {}
    public static func n() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!>
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>b<!>
        c // ok
        d // ok
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>l<!>()
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>m<!>()
        o() // ok
    }
    private static func o() {}
    public operator func +(q: A) {
        a
        b
        c
        d
        l()
        m()
        n()
        o()
    }
    private operator func -(s: A) {}
}

main() {
    A().a // ok
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>A<!>.a

    <!INVALID_ACCESS_CONTROL!>A<!>().b
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>A<!>.b

    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>A<!>().c
    A.c // ok

    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>A<!>().d
    <!INVALID_ACCESS_CONTROL!>A<!>.d

    A().m()
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>A<!>.m()

    A().<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>l<!>()
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>A<!>.l()

    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>A<!>().n()
    A.n() // ok

    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>A<!>().o()
    A.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>o<!>()

    <!REF_NOT_BE_TYPE!>A<!> <!INVALID_BINARY_OPERATOR!>+<!> <!REF_NOT_BE_TYPE!>A<!>
    A() + A()

    A() <!INVALID_BINARY_OPERATOR!>-<!> A()
    return 0
}
