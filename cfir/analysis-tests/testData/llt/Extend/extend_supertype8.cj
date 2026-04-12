// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

abstract class C {
    public func f(): Unit
}

interface I {
    func f(): Unit
}

extend C <: I {
    public func f(): Unit {} // redefinition
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'Class-C'
  ==> extend_supertype8.cj:18:17:
   | 
18 |     public func f(): Unit {} // redefinition
   |                 ^ 
   | 
note: shadowed definition of 'f' is here
  ==> extend_supertype8.cj:10:17:
   | 
10 |     public func f(): Unit
   |                 ^ 
   | 

error: 'main' is missing
 ==> extend_supertype8.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
