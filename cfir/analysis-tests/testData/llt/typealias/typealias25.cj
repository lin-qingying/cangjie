// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I {}

type X = I

interface J <: I & X {}

main() {}

/* SCAN
error: interface 'J' inherits or implements duplicate interface 'I'
  ==> typealias25.cj:13:1:
   | 
13 | interface J <: I & X {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
