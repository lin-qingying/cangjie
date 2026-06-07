// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum A<T> {
    a(T)

    public static func foo() {
        <!sema_static_function_cannot_access_non_static_member!>t<!>est()
    }
    public func test() {}
}

class B {}

func test<<!sema_upper_bound_must_be_class_or_interface!>T<!>>() where T <: Option<B> {}

main(): Int64 {
    test<<!sema_generic_type_argument_not_match_constraint!>B<!>>()
    return 1
}
