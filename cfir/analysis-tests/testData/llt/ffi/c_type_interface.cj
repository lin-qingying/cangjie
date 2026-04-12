// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {}

interface I2 {}

@C
struct R <: I1 {
    public var raw: CPointer<Unit>
    public init(raw: CPointer<Unit>) {
        this.raw = raw
    }
}

extend R <: I2 {}

/* SCAN
error: c type 'R' cannot support interface extend
  ==> c_type_interface.cj:21:8:
   | 
21 | extend R <: I2 {}
   |        ^ 
   | 

error: struct with @C cannot implement interfaces
  ==> c_type_interface.cj:14:8:
   | 
14 | struct R <: I1 {
   |        ^ 
   | 

error: 'main' is missing
 ==> c_type_interface.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
