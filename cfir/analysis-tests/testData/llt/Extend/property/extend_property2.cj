// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    public func f(): Int64 {
        0
    }
}

extend A {
    prop f: String {
        get() {
            return "C"
        }
    }
}

class B {
    public var f: String = "2"
}

extend B {
    prop f: String {
        get() {
            return "C"
        }
    }
}

extend B {
    prop f: String {
        get() {
            return "C"
        }
    }
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'Class-A'
  ==> extend_property2.cj:16:10:
   | 
16 |     prop f: String {
   |          ^ 
   | 
note: shadowed definition of 'f' is here
  ==> extend_property2.cj:10:17:
   | 
10 |     public func f(): Int64 {
   |                 ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend B'
  ==> extend_property2.cj:28:10:
   | 
28 |     prop f: String {
   |          ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend B'
  ==> extend_property2.cj:36:10:
   | 
36 |     prop f: String {
   |          ^ 
   | 

error: 'main' is missing
 ==> extend_property2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

4 errors generated, 4 errors printed.
*/
