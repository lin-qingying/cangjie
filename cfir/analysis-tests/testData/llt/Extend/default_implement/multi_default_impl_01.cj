// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

interface I4 <: I3 {}

class A {}

extend A <: I1 & I4 {}

extend A <: I3 & I2 {}

main() {
}

/* SCAN
error: unable to decide which extension happens first
  ==> multi_default_impl_01.cj:19:1:
   | 
19 | extend A <: I1 & I4 {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: conflict with this extension, beacase of 'Interface-I2 <: Interface-I1' and 'Interface-I4 <: Interface-I3'
  ==> multi_default_impl_01.cj:21:1:
   | 
21 | extend A <: I3 & I2 {}
   | ^ 
   | 

error: unable to decide which extension happens first
  ==> multi_default_impl_01.cj:21:1:
   | 
21 | extend A <: I3 & I2 {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: conflict with this extension, beacase of 'Interface-I4 <: Interface-I3' and 'Interface-I2 <: Interface-I1'
  ==> multi_default_impl_01.cj:19:1:
   | 
19 | extend A <: I1 & I4 {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
