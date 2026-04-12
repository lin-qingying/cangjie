// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var i: Int64 = 1
    ().a
    return i
}

/* SCAN
error: undeclared identifier 'a'
  ==> unit2.cj:11:8:
   | 
11 |     ().a
   |        ^ 
   | 

1 error generated, 1 error printed.
*/
