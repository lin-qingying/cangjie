// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class B {
    public var s = 1
    public func g() {}
}

class A <: B {
    public var ss = s
    public var sss = super.s
    public var ssss = super
    public var sssss = g()
    public var ssssss = g
    public var sssssss = super.g()
    public var ssssssss = super.g
    public var a: Int64
    public var b: Int64 = 1
    public var c = {
        =>
        this.b;
        b
    }
    public var d = this.f()
    public var e = this.b
    public var ff = {=> 1}
    public var gg = this
    public var fff = {=> this}
    public init() {
        func m() {
            this.b
            this
            this.f()
            b
            f()
        }

        if (false) {
            a = 1
            this.f() // OK
        }
        this.f()
        f()
        super.g()
        g()
        super.s
        s
        b
        this.b
        a = 1
        this.f()
        f()
        super.g()
        func mm() {
            this.b
            this
            this.f()
            b
            f()
        }
    }
    public func f() {}
}

/* SCAN
error: 'super' is not allowed to initialize non-static member
  ==> super_this_09.cj:16:22:
   | 
16 |     public var sss = super.s
   |                      ^ 
   | 

error: invalid super expression, it can only be used on the left-hand side of a dot
  ==> super_this_09.cj:17:23:
   | 
17 |     public var ssss = super
   |                       ^ 
   | 

error: 'super' is not allowed to initialize non-static member
  ==> super_this_09.cj:20:26:
   | 
20 |     public var sssssss = super.g()
   |                          ^ 
   | 

error: 'super' is not allowed to initialize non-static member
  ==> super_this_09.cj:21:27:
   | 
21 |     public var ssssssss = super.g
   |                           ^ 
   | 

error: 'this' is not allowed to initialize non-static member
  ==> super_this_09.cj:32:21:
   | 
32 |     public var gg = this
   |                     ^ 
   | 

error: super member 's' is not allowed to be used before calling super()
  ==> super_this_09.cj:15:21:
   | 
15 |     public var ss = s
   |                     ^ 
   | 

error: 'g' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:18:24:
   | 
18 |     public var sssss = g()
   |                        ^ 
   | 

error: 'g' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:19:25:
   | 
19 |     public var ssssss = g
   |                         ^ 
   | 

error: 'b' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:26:9:
   | 
26 |         this.b;
   |         ^ 
   | 

error: 'b' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:27:9:
   | 
27 |         b
   |         ^ 
   | 

error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:29:20:
   | 
29 |     public var d = this.f()
   |                    ^ 
   | 

error: 'this' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:33:26:
   | 
33 |     public var fff = {=> this}
   |                          ^ 
   | 

error: 'b' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:36:13:
   | 
36 |             this.b
   |             ^ 
   | 

error: 'this' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:37:13:
   | 
37 |             this
   |             ^ 
   | 

error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:38:13:
   | 
38 |             this.f()
   |             ^ 
   | 

error: 'b' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:39:13:
   | 
39 |             b
   |             ^ 
   | 

error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:40:13:
   | 
40 |             f()
   |             ^ 
   | 

error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:47:9:
   | 
47 |         this.f()
   |         ^ 
   | 

error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:48:9:
   | 
48 |         f()
   |         ^ 
   | 

error: 'g' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:49:9:
   | 
49 |         super.g()
   |         ^ 
   | 

error: 'g' is not allowed to be accessed before all member variables are initialized
  ==> super_this_09.cj:50:9:
   | 
50 |         g()
   |         ^ 
   | 

error: 'main' is missing
 ==> super_this_09.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

22 errors generated, 22 errors printed.
*/
