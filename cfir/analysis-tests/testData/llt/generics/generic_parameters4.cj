// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

open class A<T, V> {}

class B<T> <: A<T> {}

class C<T> <: A<T> {}

main() {
    return 0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> generic_parameters4.cj:11:15:
   | 
11 | class B<T> <: A<T> {}
   |               ^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> generic_parameters4.cj:13:15:
   | 
13 | class C<T> <: A<T> {}
   |               ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
