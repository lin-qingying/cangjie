// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {}

class B {}

public var (a, b): (Int64, A) = (1, A())

public var (c, d): (A, Int64) = (A(), 1)

public var (a1, b1): (B, A) = (B(), A())

/* SCAN
error: 'public' declaration uses 'internal' types
  ==> assign_007.cj:13:16:
   | 
13 | public var (a, b): (Int64, A) = (1, A())
   |                ^ inferred type 'Class-A' contains 'internal' type
   | 
note: the 'internal' type is 'Class-A'
 ==> assign_007.cj:9:7:
  | 
9 | class A {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> assign_007.cj:15:13:
   | 
15 | public var (c, d): (A, Int64) = (A(), 1)
   |             ^ inferred type 'Class-A' contains 'internal' type
   | 
note: the 'internal' type is 'Class-A'
 ==> assign_007.cj:9:7:
  | 
9 | class A {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> assign_007.cj:17:13:
   | 
17 | public var (a1, b1): (B, A) = (B(), A())
   |             ^^ inferred type 'Class-B' contains 'internal' type
   | 
note: the 'internal' type is 'Class-B'
  ==> assign_007.cj:11:7:
   | 
11 | class B {}
   |       ^ 
   | 

error: 'public' declaration uses 'internal' types
  ==> assign_007.cj:17:17:
   | 
17 | public var (a1, b1): (B, A) = (B(), A())
   |                 ^^ inferred type 'Class-A' contains 'internal' type
   | 
note: the 'internal' type is 'Class-A'
 ==> assign_007.cj:9:7:
  | 
9 | class A {}
  |       ^ 
  | 

4 errors generated, 4 errors printed.
*/
