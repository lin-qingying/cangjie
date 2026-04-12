// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a: Int32 = 1
    private var b: Int32 = a
    protected var d: Int32 = 2
    public var c: Int32 = d
}

class B <: A {
    private var e = a
    private var g = d
    private var h = c

    public var i = a
    public var k = d
    public var l = c

    protected var m = a
    protected var o = d
    protected var p = c

    public func f() {
        if (e == 1 && g == 2 && h == 2 && i == 1 && k == 2 && l == 2 && m == 1 && o == 2 && p == 2) {
            return 0
        } else {
            return 1
        }
    }
}

main(): Int64 {
    var testB = B()
    testB.f()
    return 0
}

/* SCAN
error: super member 'a' is not allowed to be used before calling super()
  ==> class_access_control7.cj:17:21:
   | 
17 |     private var e = a
   |                     ^ 
   | 

error: super member 'd' is not allowed to be used before calling super()
  ==> class_access_control7.cj:18:21:
   | 
18 |     private var g = d
   |                     ^ 
   | 

error: super member 'c' is not allowed to be used before calling super()
  ==> class_access_control7.cj:19:21:
   | 
19 |     private var h = c
   |                     ^ 
   | 

error: super member 'a' is not allowed to be used before calling super()
  ==> class_access_control7.cj:21:20:
   | 
21 |     public var i = a
   |                    ^ 
   | 

error: super member 'd' is not allowed to be used before calling super()
  ==> class_access_control7.cj:22:20:
   | 
22 |     public var k = d
   |                    ^ 
   | 

error: super member 'c' is not allowed to be used before calling super()
  ==> class_access_control7.cj:23:20:
   | 
23 |     public var l = c
   |                    ^ 
   | 

error: super member 'a' is not allowed to be used before calling super()
  ==> class_access_control7.cj:25:23:
   | 
25 |     protected var m = a
   |                       ^ 
   | 

error: super member 'd' is not allowed to be used before calling super()
  ==> class_access_control7.cj:26:23:
   | 
26 |     protected var o = d
   |                       ^ 
   | 

9 errors generated, 8 errors printed.
*/
