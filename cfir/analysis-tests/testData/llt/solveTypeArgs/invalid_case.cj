// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A<B> {
    func foo<C>(): C where C <: B {
        throw Exception("no impl")
    }
}

func bar<B, C>(a: A<B>): C where C <: B {
    throw A("no impl")
}

interface Super {}

interface Sub <: Super {}

func acceptSub(sub: Sub): Unit {
}

func use() {
    let a = acceptSub<Super>()
    a(bar.foo())
    acceptSub(bar(a))
}

main() {
}

/* SCAN
error: the upper bound 'Generics-B' of generic parameter 'Generics-C' must be class or interface
  ==> invalid_case.cj:10:14:
   | 
10 |     func foo<C>(): C where C <: B {
   |              ^ 
   | 

error: the upper bound 'Generics-B' of generic parameter 'Generics-C' must be class or interface
  ==> invalid_case.cj:15:13:
   | 
15 | func bar<B, C>(a: A<B>): C where C <: B {
   |             ^ 
   | 

error: extra argument given for parameter list '()' in call
  ==> invalid_case.cj:16:12:
   | 
16 |     throw A("no impl")
   |            ^^^^^^^^^^^ expected 0 arguments, found 1
   | 

error: type argument's number does not match type parameter's number
  ==> invalid_case.cj:27:13:
   | 
27 |     let a = acceptSub<Super>()
   |             ^^^^^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
