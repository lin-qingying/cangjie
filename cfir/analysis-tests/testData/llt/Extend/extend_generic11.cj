// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    A<Int64>.<!NOT_MEMBER_OF!>f<!>
    if (A<Int64>.<!NOT_MEMBER_OF!>a<!> != 1) {
        return 1
    }
    C<Int64>.a.<!NOT_MEMBER_OF!>a1<!>
    C<Int64>.a.<!NOT_MEMBER_OF!>f1<!>
    return 0
}

class A<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: String {
    public static prop a: Int64 {
        get() {
            return 1
        }
    }
    public static func f() {
        2
    }

    public prop a1: Int64 {
        get() {
            return 1
        }
    }
    public func f1() {
        2
    }
}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {
    public static var a: A<T> = A<T>()
}
