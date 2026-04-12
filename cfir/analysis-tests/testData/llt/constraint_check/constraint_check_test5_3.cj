// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix
// UpperBound: Tuple Type

open class Base<T> {}

open class Child<T> <: Base<T> {}

open class C<T> {}

class D <: Child<D> {}

class E <: Base<E> {}

func foo<T>() where T <: Base<T> {
    return 0
}

func Jay<T>() where T <: (Base<Int32>, Child<D>) {
    return 0
}

main(): Int64 {
    foo<D>() // D <: Child<D> <: Base<D>
    foo<E>() // E <: Base<E>
    Jay<(Child<Int32>, D)>() // Child<Int32> <: Base<Int32>, D <: Child<D>
}

/* SCAN
error: the upper bound 'Tuple<Class-Base<Int32>, Class-Child<Class-D>>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test5_3.cj:24:10:
   | 
24 | func Jay<T>() where T <: (Base<Int32>, Child<D>) {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
