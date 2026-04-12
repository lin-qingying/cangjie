// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

main() {
    var range1 = ..10 :3 
    for (i in range1) {
        i
    }

    for (i in 1..:3) {   
        i
    }
    return 0
}

/* SCAN
error: expected expression after '=', found '..'
  ==> range3.cj:10:18:
   | 
10 |     var range1 = ..10 :3 
   |                  ^^ expected expression here
   | 

error: expected expression after '..', found ':'
  ==> range3.cj:15:18:
   | 
15 |     for (i in 1..:3) {   
   |                  ^ expected expression here
   | 

2 errors generated, 2 errors printed.
*/
