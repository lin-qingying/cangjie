// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// UpperBound: Tuple Type

open class Base<T> {}

open class Child<T> <: Base<T> {}

open class C<T> {}

class D <: Child<D> {}

class E <: Base<E> {}

func foo<T>() where T <: Base<T> {
    return 0
}

func Jay<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Base<Int32>, Child<D>) {
    return 0
}

main(): Int64 {
    foo<D>() // D <: Child<D> <: Base<D>
    foo<E>() // E <: Base<E>
    Jay<(Child<Int32>, D)>() // Child<Int32> <: Base<Int32>, D <: Child<D>
}
