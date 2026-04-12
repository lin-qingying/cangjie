// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    return 0
}

class C <: A {
    public init() {
        a = 0
        this.b = 0
    }
    public init(c: Int32) {
        a = 0
        this.b = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let b: Int32
}

class D <: E {
    public init() {
        a = 0
        this.b = 0
    }
    public init(c: Int32) {
        a = 0
        this.b = 0
    }
}

open class E {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: the uninitialized member variable 'b' is not initialized in the constructor of class or struct
  ==> let_in_init10-1.cj:26:5:
   | 
26 |     public let b: Int32
   |     ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init10-1.cj:32:9:
   | 
32 |         this.b = 0
   |         ^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init10-1.cj:42:16:
   | 
42 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init10-1.cj:36:9:
   | 
36 |         this.b = 0
   |         ^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init10-1.cj:42:16:
   | 
42 |     public let b: Int32 = 1
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
