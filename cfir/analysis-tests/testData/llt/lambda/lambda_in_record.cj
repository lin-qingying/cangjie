// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
struct Day {
    public let width: Int32 = 1
    public let length: Int32 = 2
    public let perimeter2: (Unit) -> Int32 = {=> var res: Int32 = 0}
}

/* SCAN
error: mismatched types
  ==> lambda_in_record.cj:11:46:
   | 
11 |     public let perimeter2: (Unit) -> Int32 = {=> var res: Int32 = 0}
   |                                              ^^^^^^^^^^^^^^^^^^^^^^^ expected '(Unit) -> Int32', found '() -> Unit'
   | 

1 error generated, 1 error printed.
*/
