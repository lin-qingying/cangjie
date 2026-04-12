// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class A {
    public open prop f: Int32 {
        get() {
            return 0
        }
    }
    public init() {
    }
}

open class B <: A {
    public init() {
        f
        var a = {=> f}
        func f3(c!: Int32 = f) {
            f
        }
        a()
    }
    public open override prop f: Int32 {
        get() {
            return 2
        }
    }
}

class C <: B {
    public init() {
        super.f
        var a = {=> f}
        func f3(c!: Int32 = f) {
            this.f
        }
        B().f
        a()
    }
}

main(): Int64 {
    0
}

/* SCAN
error: instance member property 'f' cannot be accessed in the constructor of open class 'B'
  ==> class_open_modifier4.cj:21:9:
   | 
21 |         f
   |         ^ 
   | 

error: instance member property 'f' cannot be accessed in the constructor of open class 'B'
  ==> class_open_modifier4.cj:22:21:
   | 
22 |         var a = {=> f}
   |                     ^ 
   | 

error: instance member property 'f' cannot be accessed in the constructor of open class 'B'
  ==> class_open_modifier4.cj:23:29:
   | 
23 |         func f3(c!: Int32 = f) {
   |                             ^ 
   | 

error: instance member property 'f' cannot be accessed in the constructor of open class 'B'
  ==> class_open_modifier4.cj:24:13:
   | 
24 |             f
   |             ^ 
   | 

4 errors generated, 4 errors printed.
*/
