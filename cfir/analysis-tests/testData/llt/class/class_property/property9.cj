// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class A {
    public mut open prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

interface B {
    mut prop i: Int32
}

class C <: A & B {
    public mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

open class A1 {
    public mut open prop i: String {
        get() {
            return "0"
        }
        set(v) {}
    }
}

interface B1 {
    mut prop i: Int64
}

class C1 <: A1 & B1 {
    public mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

/* SCAN
error: The type of the override/implement property must be the same
  ==> property9.cj:23:5:
   | 
23 |     public mut override prop i: Int64 {
   |     ^ 
   | 

error: The type of the override/implement property must be the same
  ==> property9.cj:45:5:
   | 
45 |     public mut override prop i: Int64 {
   |     ^ 
   | 

error: 'main' is missing
 ==> property9.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
