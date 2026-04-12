// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

type D = F

type F = D

main() {
    return 0
}

/* SCAN
error: type cycle detected: 'D->F->D'
 ==> typealias6.cj:9:1:
  | 
9 | type D = F
  | ^ 
  | 

error: type cycle detected: 'F->D->F'
  ==> typealias6.cj:11:1:
   | 
11 | type F = D
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
