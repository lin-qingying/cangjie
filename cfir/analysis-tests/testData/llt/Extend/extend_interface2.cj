// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// if both two functions with the same signature have default implementations, they must be overridden in the extend decl
interface A {
    func add(): Unit {}
}

interface B {
    func add(): Unit {}
}

class S {}

extend S <: A & B {}

/* SCAN
error: interface function 'add' must be implemented in 'extend S'
  ==> extend_interface2.cj:20:1:
   | 
20 | extend S <: A & B {}
   | ^ 
   | 

error: 'main' is missing
 ==> extend_interface2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
