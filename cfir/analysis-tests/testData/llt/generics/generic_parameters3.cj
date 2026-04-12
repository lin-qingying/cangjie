// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A<T> {}

class B<T, V> <: A<T, V> {}

class C<T, V> <: A<V, T> {}

class D<T1, T2, T3> <: A<T1, T2, T3> {}

main() {
    return 0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> generic_parameters3.cj:11:18:
   | 
11 | class B<T, V> <: A<T, V> {}
   |                  ^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> generic_parameters3.cj:13:18:
   | 
13 | class C<T, V> <: A<V, T> {}
   |                  ^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> generic_parameters3.cj:15:24:
   | 
15 | class D<T1, T2, T3> <: A<T1, T2, T3> {}
   |                        ^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
