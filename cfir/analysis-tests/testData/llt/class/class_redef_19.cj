// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C<T> {}

open class A {
    public static func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a: C<T>) where T <: Int32 {
        return 1
    }
}

public open class B <: A {
    public static redef func <!ACCESSIBILITY_ERROR!>foo<!><<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a: C<T>) where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: Int64<!> {
        return 1
    }
}

main() {
    return 0
}
