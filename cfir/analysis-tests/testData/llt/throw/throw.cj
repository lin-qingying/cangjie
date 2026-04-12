// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class MyException <: Exception {}

main(): Int64 {
    throw Exception() // ok
    throw MyException() // ok
    throw 1
    return 0
}

/* SCAN
error: the object thrown must derive from `core.Exception`
  ==> throw.cj:14:5:
   | 
14 |     throw 1
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
