// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %typecheck %cmp_opt %f

class A<T> {
    public init(a!: A<T> = A<T>()) {}
}

/* SCAN
error: recursive constructor calling detected
  ==> class_generic_recursive_function_call01.cj:10:12:
   | 
10 |     public init(a!: A<T> = A<T>()) {}
   |            ^^^^ 
   | 
note: depends on
  ==> class_generic_recursive_function_call01.cj:10:28:
   | 
10 |     public init(a!: A<T> = A<T>()) {}
   |                            ^^^^ 
   | 
note: depends on
  ==> class_generic_recursive_function_call01.cj:10:12:
   | 
10 |     public init(a!: A<T> = A<T>()) {}
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/
