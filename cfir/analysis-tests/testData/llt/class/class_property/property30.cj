// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class Boo {
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var a: Int32 = 1 // a = 1
    public func f(): Int32 {
        m = 2
        0
    }
}

class Doo <: Boo {
    public var b: Int32 = n
    public var c: Unit = (m = 1)
    public var d: Int32 = n
    public var e: Int32 = m
    public var e1: Int32 = super.m
    public func f1(): Int32 {
        m = 2
        0
    }
}

/* SCAN
error: 'super' is not allowed to initialize non-static member
  ==> property30.cj:35:28:
   | 
35 |     public var e1: Int32 = super.m
   |                            ^ 
   | 

error: 'n' is not allowed to be accessed before all member variables are initialized
  ==> property30.cj:31:27:
   | 
31 |     public var b: Int32 = n
   |                           ^ 
   | 

error: 'm' is not allowed to be accessed before all member variables are initialized
  ==> property30.cj:32:27:
   | 
32 |     public var c: Unit = (m = 1)
   |                           ^ 
   | 

error: 'n' is not allowed to be accessed before all member variables are initialized
  ==> property30.cj:33:27:
   | 
33 |     public var d: Int32 = n
   |                           ^ 
   | 

error: 'm' is not allowed to be accessed before all member variables are initialized
  ==> property30.cj:34:27:
   | 
34 |     public var e: Int32 = m
   |                           ^ 
   | 

error: 'main' is missing
 ==> property30.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

6 errors generated, 6 errors printed.
*/
