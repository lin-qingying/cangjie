// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

open class Base<T> {
    public var i: Int32 = 2
    public var j: Int32 = 1
    public func foo(): Int32 {
        return 3
    }
    public init() {}
    public init(a: Int32) {}
}

class Test <: Base<Int32> {
    public var i: Int32 = 1
    public init(a: Int32) {}
    public init(a: Test) {}
    public init() {
        this(this.i)
    }
    public init(a: Int32, b: Int32) {
        this(this.f())
    }
    public init(a: Int32, b: Int32, c: Int32) {
        this(i)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32) {
        this(f())
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32) {
        this(this)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(super.i)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(super.foo())
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(j)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(foo())
    }
    public func f(): Int32 {
        return 0
    }
}

/* SCAN
error: constructor 'init' has overload conflicts
  ==> super_this_10.cj:47:12:
   | 
47 |     public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
   |            ^^^^ 
   | 
note: conflict with the declaration
  ==> super_this_10.cj:41:12:
   | 
41 |     public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
   |            ^^^^ 
   | 

error: constructor 'init' has overload conflicts
  ==> super_this_10.cj:44:12:
   | 
44 |     public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
   |            ^^^^ 
   | 
note: conflict with the declaration
  ==> super_this_10.cj:38:12:
   | 
38 |     public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
   |            ^^^^ 
   | 

error: 'this' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:24:14:
   | 
24 |         this(this.i)
   |              ^ 
   | 

error: 'this' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:27:14:
   | 
27 |         this(this.f())
   |              ^ 
   | 

error: 'i' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:30:14:
   | 
30 |         this(i)
   |              ^ 
   | 

error: 'f' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:33:14:
   | 
33 |         this(f())
   |              ^ 
   | 

error: 'this' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:36:14:
   | 
36 |         this(this)
   |              ^ 
   | 

error: 'super' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:39:15:
   | 
39 |         super(super.i)
   |               ^ 
   | 

error: 'super' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:42:15:
   | 
42 |         super(super.foo())
   |               ^ 
   | 

error: 'j' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:45:15:
   | 
45 |         super(j)
   |               ^ 
   | 

error: 'foo' is not allowed to be used in arguments of constructor call
  ==> super_this_10.cj:48:15:
   | 
48 |         super(foo())
   |               ^ 
   | 

error: the variable 'i' must not shadow a member variable of the supertype
  ==> super_this_10.cj:20:5:
   | 
20 |     public var i: Int32 = 1
   |     ^ 
   | 

error: 'main' is missing
 ==> super_this_10.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

13 errors generated, 13 errors printed.
*/
