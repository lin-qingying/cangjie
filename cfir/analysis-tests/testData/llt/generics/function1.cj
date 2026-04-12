// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func Add<T>(a: T): T {
    return a
}

main() {
    var a: Int64 = 1
    Add<Float64>(a)
    Add<Int64>(a)
    Add<T>(a)
    Add(1)
}

/* SCAN
error: undeclared type name 'T'
  ==> function1.cj:17:9:
   | 
17 |     Add<T>(a)
   |         ^ 
   | 

error: mismatched types
  ==> function1.cj:15:18:
   | 
15 |     Add<Float64>(a)
   |                  ^ expected 'Float64', found 'Int64'
   | 

2 errors generated, 2 errors printed.
*/
