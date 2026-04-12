// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface Foo<T> {}

extend Int64 <: Foo {}

/* SCAN
error: generic type should be used with type argument
  ==> extend_interface6.cj:11:17:
   | 
11 | extend Int64 <: Foo {}
   |                 ^ 
   | 

error: 'main' is missing
 ==> extend_interface6.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
