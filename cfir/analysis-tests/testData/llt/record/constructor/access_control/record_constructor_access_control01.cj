// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct A {
    public let b: Int64 = 1
    public init() {
        b = 1
        this.b = 2
    }
}

main(): Int64 {
    0
}

/* SCAN
error: cannot assign to immutable value
  ==> record_constructor_access_control01.cj:12:9:
   | 
12 |         b = 1
   |         ^^^^^ 
   | 
note: variable 'b' is immutable
  ==> record_constructor_access_control01.cj:10:16:
   | 
10 |     public let b: Int64 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> record_constructor_access_control01.cj:13:9:
   | 
13 |         this.b = 2
   |         ^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> record_constructor_access_control01.cj:10:16:
   | 
10 |     public let b: Int64 = 1
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
