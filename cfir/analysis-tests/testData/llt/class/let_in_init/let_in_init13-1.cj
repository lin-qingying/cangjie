// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

class C <: A {
    public init() {
        a = 0
        b = 0
    }
    public init(c: Int32) {
        a = 0
        b = 0
    }
}

open class A {
    public mut prop a: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
    public prop b: Int64 {
        get() {
            return 0
        }
    }
    public init() {
        b = 1
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: instance member property 'b' cannot be accessed in the constructor of open class 'A'
  ==> let_in_init13-1.cj:33:9:
   | 
33 |         b = 1
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init13-1.cj:33:9:
   | 
33 |         b = 1
   |         ^^^^^ 
   | 
note: property 'b' is immutable
  ==> let_in_init13-1.cj:27:17:
   | 
27 |     public prop b: Int64 {
   |                 ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init13-1.cj:12:9:
   | 
12 |         b = 0
   |         ^^^^^ 
   | 
note: property 'b' is immutable
  ==> let_in_init13-1.cj:27:17:
   | 
27 |     public prop b: Int64 {
   |                 ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init13-1.cj:16:9:
   | 
16 |         b = 0
   |         ^^^^^ 
   | 
note: property 'b' is immutable
  ==> let_in_init13-1.cj:27:17:
   | 
27 |     public prop b: Int64 {
   |                 ^ 
   | 

4 errors generated, 4 errors printed.
*/
