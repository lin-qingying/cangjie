// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public mut prop a: Int64 {
        get() {
            1
        }
        set(v)(x) {}
    }
}

class B {
    public mut prop a: Int64 {
        get(v) {
            1
        }
        set(v) {}
    }
}

class C {
    public mut prop a: Int64 {
        get()() {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}

/* SCAN
error: setter cannot have more than one parameter list
  ==> property15.cj:14:9:
   | 
14 |         set(v)(x) {}
   |         ^ 
   | 

error: getter cannot have parameter
  ==> property15.cj:20:9:
   | 
20 |         get(v) {
   |         ^ 
   | 

error: getter cannot have more than one parameter list
  ==> property15.cj:29:9:
   | 
29 |         get()() {
   |         ^ 
   | 

3 errors generated, 3 errors printed.
*/
