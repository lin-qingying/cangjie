// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var i8Number1: Int8 = 127
var i16Number1: Int16 = 0

func funcTest3(): Unit {
    i8Number1 = Int16(i16Number1) // err
    i8Number1 = i16Number1
    i8Number1 = Int16(i16Number1) + i16Number1 + Int16(i16Number1) // err
}

/* SCAN
error: mismatched types
  ==> UserTest4.cj:13:17:
   | 
13 |     i8Number1 = Int16(i16Number1) // err
   |                 ^^^^^^^^^^^^^^^^^ expected 'Int8', found 'Int16'
   | 

error: mismatched types
  ==> UserTest4.cj:14:17:
   | 
14 |     i8Number1 = i16Number1
   |                 ^^^^^^^^^^ expected 'Int8', found 'Int16'
   | 

error: mismatched types
  ==> UserTest4.cj:15:17:
   | 
15 |     i8Number1 = Int16(i16Number1) + i16Number1 + Int16(i16Number1) // err
   |                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Int8', found 'Int16'
   | 

3 errors generated, 3 errors printed.
*/
