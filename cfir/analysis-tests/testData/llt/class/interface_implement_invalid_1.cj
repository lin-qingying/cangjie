// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {}

interface C1 <: I2 {}

class C2 <: I2 {}

class C3 <: I2 {
    public func foo(a: Int64): Int64 {
        return 0
    }
}

class C4 <: I2 {
    public func foo(): Int64 {
        return 0
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: class 'C2' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> interface_implement_invalid_1.cj:17:1:
   | 
17 | class C2 <: I2 {}
   | ^ 
   | 
note: unimplemented interface function 'foo'
  ==> interface_implement_invalid_1.cj:10:5:
   | 
10 |     func foo(): Int64
   |     ^ 
   | 

error: class 'C3' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> interface_implement_invalid_1.cj:19:1:
   | 
19 | class C3 <: I2 {
   | ^ 
   | 
note: unimplemented interface function 'foo'
  ==> interface_implement_invalid_1.cj:10:5:
   | 
10 |     func foo(): Int64
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
