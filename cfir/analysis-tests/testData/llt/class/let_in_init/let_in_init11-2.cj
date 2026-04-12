// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    return 0
}

class B <: A {
    public init() {
        a = 0
        b = 0
    }

    public func foo1() {
        a = 0
        b = 0
    }

    public func foo2() {
        this.a = 0
        this.b = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init11-2.cj:16:9:
   | 
16 |         b = 0
   |         ^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init11-2.cj:32:16:
   | 
32 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init11-2.cj:21:9:
   | 
21 |         b = 0
   |         ^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init11-2.cj:32:16:
   | 
32 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init11-2.cj:26:9:
   | 
26 |         this.b = 0
   |         ^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init11-2.cj:32:16:
   | 
32 |     public let b: Int32 = 1
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
