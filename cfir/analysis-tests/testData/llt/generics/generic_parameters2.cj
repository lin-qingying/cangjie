// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<X, Y> {}

open class B<X, Y> <: A<X, Y> {}

open class C<X, Y> <: A<X, Y> {}

func foo<Xot>() {}

open class <!CLASSIFIER_REDECLARATION!>A<!><X, Y> {}

open class <!CLASSIFIER_REDECLARATION!>B<!><X, Y> <: <!AMBIGUOUS_USE!>A<X, Y><!> {}

open class <!CLASSIFIER_REDECLARATION!>C<!><X, Y> <: <!AMBIGUOUS_USE!>A<X, <!UNRESOLVED_REFERENCE!>Z<!>, Y><!> {}

func foo<X, Y>(a: B<Y, X>, b: C<Y, X>): Int64 {
    0
}

main() {
    foo(B<Int32, Int64>(), C<Int32, Int64>())
}
