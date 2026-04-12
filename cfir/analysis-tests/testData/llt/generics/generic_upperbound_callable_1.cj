// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

func foo<T>(f: T) where T <: (Int) -> Int {
    func bar<U>(g: U) where U <: T {
        func baz<R>(h: R) where R <: U {
            h(0)
        }
        g(1)
        baz(g)
    }
    f(2)
    bar(f)
}

main() {
    foo({x: Int => x})
}

/* SCAN
error: the upper bound '(Int64) -> Int64' of generic parameter 'Generics-T' must be class or interface
 ==> generic_upperbound_callable_1.cj:9:10:
  | 
9 | func foo<T>(f: T) where T <: (Int) -> Int {
  |          ^ 
  | 

error: the upper bound '(Int64) -> Int64' of generic parameter 'Generics-U' must be class or interface
  ==> generic_upperbound_callable_1.cj:10:14:
   | 
10 |     func bar<U>(g: U) where U <: T {
   |              ^ 
   | 

error: the upper bound '(Int64) -> Int64' of generic parameter 'Generics-R' must be class or interface
  ==> generic_upperbound_callable_1.cj:11:18:
   | 
11 |         func baz<R>(h: R) where R <: U {
   |                  ^ 
   | 

3 errors generated, 3 errors printed.
*/
