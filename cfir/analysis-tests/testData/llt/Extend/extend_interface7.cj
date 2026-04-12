// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {}

extend Int64 <: Int64 {}

extend A <: Int64 {}

extend Int64 <: A {}

extend A <: A {}

extend A <: Int64 & A {}

/* SCAN
error: expected an interface, found non-interface type
  ==> extend_interface7.cj:11:17:
   | 
11 | extend Int64 <: Int64 {}
   |                 ^^^^^ expected an interface here
   | 

error: expected an interface, found non-interface type
  ==> extend_interface7.cj:13:13:
   | 
13 | extend A <: Int64 {}
   |             ^^^^^ expected an interface here
   | 

error: expected an interface, found non-interface type
  ==> extend_interface7.cj:15:17:
   | 
15 | extend Int64 <: A {}
   |                 ^ expected an interface here
   | 

error: expected an interface, found non-interface type
  ==> extend_interface7.cj:17:13:
   | 
17 | extend A <: A {}
   |             ^ expected an interface here
   | 

error: expected an interface, found non-interface type
  ==> extend_interface7.cj:19:13:
   | 
19 | extend A <: Int64 & A {}
   |             ^^^^^ expected an interface here
   | 

error: expected an interface, found non-interface type
  ==> extend_interface7.cj:19:21:
   | 
19 | extend A <: Int64 & A {}
   |                     ^ expected an interface here
   | 

error: 'main' is missing
 ==> extend_interface7.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

7 errors generated, 7 errors printed.
*/
