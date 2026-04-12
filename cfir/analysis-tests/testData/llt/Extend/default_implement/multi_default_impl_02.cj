// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {}

interface I2 <: I1 {}

interface I5 <: I2 {}

class B {}

extend B <: I2 {}

extend B <: I1 & I5 {}

main() {
}

/* SCAN
error: unable to decide which extension happens first
  ==> multi_default_impl_02.cj:17:1:
   | 
17 | extend B <: I2 {}
   | ^^^^^^^^^^^^^^^^^ 
   | 
note: conflict with this extension, beacase of 'Interface-I5 <: Interface-I2 <: Interface-I1'
  ==> multi_default_impl_02.cj:19:1:
   | 
19 | extend B <: I1 & I5 {}
   | ^ 
   | 

error: unable to decide which extension happens first
  ==> multi_default_impl_02.cj:19:1:
   | 
19 | extend B <: I1 & I5 {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: conflict with this extension, beacase of 'Interface-I2 <: Interface-I1' and 'Interface-I5 <: Interface-I2'
  ==> multi_default_impl_02.cj:17:1:
   | 
17 | extend B <: I2 {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
