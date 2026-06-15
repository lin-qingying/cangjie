// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum A<T> {
    a(T)

    public static func foo() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>t<!>est()
    }
    public func test() {}
}

class B {}

func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Option<B> {}

main(): Int64 {
    test<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>B<!>>()
    return 1
}
