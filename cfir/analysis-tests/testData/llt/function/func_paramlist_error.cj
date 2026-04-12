// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct A<T>{
    public operator func []<b:Float32):Bool {true}
}
main() {}

/* SCAN
error: unclosed delimiter: '<'
  ==> func_paramlist_error.cj:10:30:
   | 
10 |     public operator func []<b:Float32):Bool {true}
   |                            ~ ^ expected '>' here
   |                            |
   |                            to match this opening '<'
   | 

error: expected declaration, found ')'
  ==> func_paramlist_error.cj:10:38:
   | 
10 |     public operator func []<b:Float32):Bool {true}
   |                                      ^ expected declaration here
   | 

2 errors generated, 2 errors printed.
*/
