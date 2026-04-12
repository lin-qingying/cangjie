// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix
// UpperBound: Tuple Type

func foo<T>() where T <: (Int32, Int32) {
    return 0
}

open class Base {}

class Child <: Base {}

func bar<T>() where T <: (Int32, Base) {
    return 0
}

main(): Int64 {
    foo<(Int32, Int32)>()
    bar<(Int32, Base)>()
    bar<(Int32, Child)>() // Tuple type is covariant
}

/* SCAN
error: the upper bound 'Tuple<Int32, Int32>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test5_1.cj:10:10:
   | 
10 | func foo<T>() where T <: (Int32, Int32) {
   |          ^ 
   | 

error: the upper bound 'Tuple<Int32, Class-Base>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test5_1.cj:18:10:
   | 
18 | func bar<T>() where T <: (Int32, Base) {
   |          ^ 
   | 

2 errors generated, 2 errors printed.
*/
