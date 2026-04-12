// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(a: A): Int32 {
    123
    return 1
    1.2
    return "hello"
}

class A {}

func bar(b: Int32): Int32 {
    123
    1
    12.0
}

/* SCAN
error: mismatched types
  ==> funcdecl1.cj:13:12:
   | 
13 |     return "hello"
   |            ^^^^^^^ expected 'Int32', found 'Struct-String'
   | 

error: cannot convert a floating-point literal to type 'Int32'
  ==> funcdecl1.cj:21:5:
   | 
21 |     12.0
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
