// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class Parent {
    public Parent(c!: Int64 = 1, var a!: Int32 = 1) {}
}

class children <: Parent {
    public children(c!: Int32 = this.a) {}
    public init(c: Int64, b!: Int32 = super.a) {}
    public func f(d!: Int32 = this.a) {}
    public func f1(d!: Int32 = super.a) {}
}

struct A {
    public var a: Int64 = 1
    public init(e!: Int64 = super.a) {}
    public init(b!: String = "a", c!: Int64 = this.a) {}
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'super' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor6.cj:15:39:
   | 
15 |     public init(c: Int64, b!: Int32 = super.a) {}
   |                                       ^ 
   | 

error: 'this' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor6.cj:14:33:
   | 
14 |     public children(c!: Int32 = this.a) {}
   |                                 ^ 
   | 

error: 'super' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor6.cj:22:29:
   | 
22 |     public init(e!: Int64 = super.a) {}
   |                             ^ 
   | 

error: 'this' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor6.cj:23:47:
   | 
23 |     public init(b!: String = "a", c!: Int64 = this.a) {}
   |                                               ^ 
   | 

4 errors generated, 4 errors printed.
*/
