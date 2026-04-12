// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

interface I {}

class A<X> {}

extend<V> A<V> <: I where V <: I {}

func f<T>() where T <: A<T> & A<Int64> {}

main() {}

/* SCAN
error: generic parameter 'T' cannot have two or more class upper bounds 'Class-A<Generics-T>, Class-A<Int64>' without subtype relation
  ==> subtype_00.cj:15:8:
   | 
15 | func f<T>() where T <: A<T> & A<Int64> {}
   |        ^ 
   | 

1 error generated, 1 error printed.
*/
