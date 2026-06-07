// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// UpperBound: Tuple Type

func foo<T>() where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>(Int32, Int32)<!> {
    return 0
}

open class Base {}

class Child <: Base {}

class C {}

func bar<T>() where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>(Int32, Child)<!> {
    return 0
}

main(): Int64 {
    <!GENERIC_ARGUMENT_NO_MATCH!>foo<(Int32, Int64)>()<!>
    <!GENERIC_ARGUMENT_NO_MATCH!>bar<(Int32, Base)>()<!>
    bar<(Int32, Child)>() // OK
    <!GENERIC_ARGUMENT_NO_MATCH!>bar<(Int32, C)>()<!>
    <!GENERIC_ARGUMENT_NO_MATCH!>bar<(Int64, Child)>()<!>
    <!GENERIC_ARGUMENT_NO_MATCH!>bar<(C, Child)>()<!>
}
