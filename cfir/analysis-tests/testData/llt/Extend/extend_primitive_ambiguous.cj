// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend Int32 {
    func b() {}
}

extend Int8 {
    func b() {}
}

var a = 1.b()

/* SCAN
error: ambiguous match for function call 'b' of these extended type: Int8 Int32
  ==> extend_primitive_ambiguous.cj:17:9:
   | 
17 | var a = 1.b()
   |         ^^^ 
   | 

error: 'main' is missing
 ==> extend_primitive_ambiguous.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
