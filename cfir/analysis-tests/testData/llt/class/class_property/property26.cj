// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    var a = Coo()
    if (a.n != 1) {
        return 1
    }
    a.m = 10
    if (a.m != 2) {
        return 2
    }
    return 0
}

abstract class Boo {
    public mut prop m: Int32
    public prop n: Int32
}

extend Boo {
    public mut prop m: Int32 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop n: Int32 {
        get() {
            return 1
        }
    }
}

class Coo <: Boo {
    public mut prop m: Int32 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop n: Int32 {
        get() {
            return 1
        }
    }
}

/* SCAN
error: extend member 'm' is not allowed to shadow members of 'Class-Boo'
  ==> property26.cj:27:21:
   | 
27 |     public mut prop m: Int32 {
   |                     ^ 
   | 
note: shadowed definition of 'm' is here
  ==> property26.cj:22:21:
   | 
22 |     public mut prop m: Int32
   |                     ^ 
   | 

error: extend member 'n' is not allowed to shadow members of 'Class-Boo'
  ==> property26.cj:33:17:
   | 
33 |     public prop n: Int32 {
   |                 ^ 
   | 
note: shadowed definition of 'n' is here
  ==> property26.cj:23:17:
   | 
23 |     public prop n: Int32
   |                 ^ 
   | 

error: cannot override property 'm' in extend of supertype
  ==> property26.cj:41:21:
   | 
41 |     public mut prop m: Int32 {
   |                     ^ 
   | 
note: overridden definition of 'm' is here
  ==> property26.cj:27:21:
   | 
27 |     public mut prop m: Int32 {
   |                     ^ 
   | 

error: cannot override property 'n' in extend of supertype
  ==> property26.cj:47:17:
   | 
47 |     public prop n: Int32 {
   |                 ^ 
   | 
note: overridden definition of 'n' is here
  ==> property26.cj:33:17:
   | 
33 |     public prop n: Int32 {
   |                 ^ 
   | 

4 errors generated, 4 errors printed.
*/
