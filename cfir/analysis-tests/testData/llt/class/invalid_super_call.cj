// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f %error_count_limit_all 2>&1 | compare %f

func mm() {
    super()
    super
}

open class c {
    var c: Int64 = 1
}

class m <: c {
    public var a = super // ok
    public var b = super()
    public var c: Int64 = super()
    public static var d = super()
    public static var e: Int64 = super()
    public init() {
        super() // ok
    }
    public init(a: Int64) {
        super // ok
    }
    public init(a: Int64, b: Int64) {
        super().a
        super().c
    }
    public init(a: Int64, b: Int64, c: Int64) {
        super().c++
    }
    public func f() {
        super()
        super().c
        super().c++
    }
    public static func ff() {
        super()
        super().c
        super().c++
    }
}

/* SCAN
error: 'super' cannot be used outside class or struct or interface
  ==> invalid_super_call.cj:10:5:
   | 
10 |     super()
   |     ^ 
   | 

error: 'super' cannot be used outside class or struct or interface
  ==> invalid_super_call.cj:11:5:
   | 
11 |     super
   |     ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:20:20:
   | 
20 |     public var b = super()
   |                    ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:21:27:
   | 
21 |     public var c: Int64 = super()
   |                           ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:22:27:
   | 
22 |     public static var d = super()
   |                           ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:23:34:
   | 
23 |     public static var e: Int64 = super()
   |                                  ^ 
   | 

error: call to 'super' must be first expression in constructor of class 'm'
  ==> invalid_super_call.cj:31:9:
   | 
31 |         super().a
   |         ^ 
   | 

error: call to 'super' must be first expression in constructor of class 'm'
  ==> invalid_super_call.cj:32:9:
   | 
32 |         super().c
   |         ^ 
   | 

error: 'a' is not a member of class 'c'
  ==> invalid_super_call.cj:31:17:
   | 
31 |         super().a
   |                 ^ 
   | 

error: call to 'super' must be first expression in constructor of class 'm'
  ==> invalid_super_call.cj:35:9:
   | 
35 |         super().c++
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:38:9:
   | 
38 |         super()
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:39:9:
   | 
39 |         super().c
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:40:9:
   | 
40 |         super().c++
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:43:9:
   | 
43 |         super()
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:44:9:
   | 
44 |         super().c
   |         ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call.cj:45:9:
   | 
45 |         super().c++
   |         ^ 
   | 

error: invalid super expression, it can only be used on the left-hand side of a dot
  ==> invalid_super_call.cj:19:20:
   | 
19 |     public var a = super // ok
   |                    ^ 
   | 

error: invalid super expression, it can only be used on the left-hand side of a dot
  ==> invalid_super_call.cj:28:9:
   | 
28 |         super // ok
   |         ^ 
   | 

error: the variable 'c' must not shadow a member variable of the supertype
  ==> invalid_super_call.cj:21:5:
   | 
21 |     public var c: Int64 = super()
   |     ^ 
   | 

19 errors generated, 19 errors printed.
*/
