// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

class A<B> {
    func foo<C>(): C where C <: B {
        throw Exception("no impl")
    }
}

func bar<B, C>(a: A<B>): C where C <: B {
    throw Exception("no impl")
}

interface Super {}

interface Sub <: Super {}

func acceptSub(sub: Sub): Unit {
}

func use() {
    let a = A<Super>()
    acceptSub(a.foo())
    acceptSub(bar(a))
}

main() {}

/* SCAN
error: the upper bound 'Generics-B' of generic parameter 'Generics-C' must be class or interface
  ==> call_inference_10.cj:10:14:
   | 
10 |     func foo<C>(): C where C <: B {
   |              ^ 
   | 

error: the upper bound 'Generics-B' of generic parameter 'Generics-C' must be class or interface
  ==> call_inference_10.cj:15:13:
   | 
15 | func bar<B, C>(a: A<B>): C where C <: B {
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
