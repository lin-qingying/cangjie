// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var _ = 9223372036854775808 % 9223372036854775808
    var _ = 9223372036854775808 / 9223372036854775808
    // Test for right value which is overflowed to 0
    let _ = -9223372036854775808 % -9223372036854775809
    let _ = -9223372036854775808 % 9223372036854775808
    let _ = -9223372036854775808 / -9223372036854775809
    let _ = -9223372036854775808 / 9223372036854775808
    return 0
}

/* SCAN
error: the number '9223372036854775808' exceeds the value range of type 'Int64'
  ==> mod_overflow.cj:10:35:
   | 
10 |     var _ = 9223372036854775808 % 9223372036854775808
   |                                   ^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '9223372036854775808' exceeds the value range of type 'Int64'
  ==> mod_overflow.cj:11:35:
   | 
11 |     var _ = 9223372036854775808 / 9223372036854775808
   |                                   ^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775809' exceeds the value range of type 'Int64'
  ==> mod_overflow.cj:13:36:
   | 
13 |     let _ = -9223372036854775808 % -9223372036854775809
   |                                    ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775808' exceeds the value range of type 'UInt64'
  ==> mod_overflow.cj:14:13:
   | 
14 |     let _ = -9223372036854775808 % 9223372036854775808
   |             ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775809' exceeds the value range of type 'Int64'
  ==> mod_overflow.cj:15:36:
   | 
15 |     let _ = -9223372036854775808 / -9223372036854775809
   |                                    ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775808' exceeds the value range of type 'UInt64'
  ==> mod_overflow.cj:16:13:
   | 
16 |     let _ = -9223372036854775808 / 9223372036854775808
   |             ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775808' exceeds the value range of type 'UInt64'
  ==> mod_overflow.cj:13:13:
   | 
13 |     let _ = -9223372036854775808 % -9223372036854775809
   |             ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775808' exceeds the value range of type 'UInt64'
  ==> mod_overflow.cj:15:13:
   | 
15 |     let _ = -9223372036854775808 / -9223372036854775809
   |             ^^^^^^^^^^^^^^^^^^^^ 
   | 

8 errors generated, 8 errors printed.
*/
