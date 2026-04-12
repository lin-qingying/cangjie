// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
interface I<T> {}

open class A<T> {}

class B<T> <: A<T> & I<T> {}

foreign func assert(input: B<Int32>): Int32

/* SCAN
error: arguments type of CFunc must be instantiated with CType
  ==> foreign_2.cj:14:21:
   | 
14 | foreign func assert(input: B<Int32>): Int32
   |                     ^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
