// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

struct R {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    var c: Int32 = 0
    let b = R().a
}

/* SCAN
error: mismatched types
  ==> record_access_control3.cj:17:5:
   | 
17 |     let b = R().a
   |     ^^^^^^^^^^^^^ expected 'Int64', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

error: can not access function 'a'
  ==> record_access_control3.cj:17:13:
   | 
17 |     let b = R().a
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
