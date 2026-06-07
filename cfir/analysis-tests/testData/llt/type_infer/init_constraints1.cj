// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C1 {}

class A<T> where T <: C1 {}

class B<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>> where X <: A<X> {}

class D<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>, V> where U <: B<U>, V <: A<V> {}

class E<T> where T <: E<T> {}

class F<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: G<T> {}

class G<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: F<T> {}

class I<T, X> where T <: C1 {}
