// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

func doo() {}

func doo(i: Int64): Int32 {
    return 1
}

func doo(i: Int64): Int32 {
    return 1
}

func doo(i: Int32) {}

func doo(i: Float32) {}

/* SCAN
error: function 'doo' has overload conflicts
  ==> redefinition1.cj:15:6:
   | 
15 | func doo(i: Int64): Int32 {
   |      ^^^ 
   | 
note: conflict with the declaration
  ==> redefinition1.cj:11:6:
   | 
11 | func doo(i: Int64): Int32 {
   |      ^^^ 
   | 

1 error generated, 1 error printed.
*/
