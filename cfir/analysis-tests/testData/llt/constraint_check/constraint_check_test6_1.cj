// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// UpperBound: Array Type

func foo<T>() where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Array<Int32><!> {
    return 0
}

open class Base {}

class Child <: Base {}

func bar<T>() where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Array<(Int32, Base)><!> {
    return 0
}

func jay<T>() where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>(Int32, Array<Base>)<!> {
    return 0
}

main(): Int64 {
    foo<Array<Int32>>()
    bar<Array<(Int32, Base)>>()
    <!GENERIC_ARGUMENT_NO_MATCH!>bar<Array<(Int32, Child)>>()<!>
    jay<(Int32, Array<Base>)>()
    <!GENERIC_ARGUMENT_NO_MATCH!>jay<(Int32, Array<Child>)>()<!>
}
