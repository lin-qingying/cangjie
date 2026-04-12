// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum B {
    a(Int64,Int64,Int64) | b

    public mut prop b : Int64 { 
       get() {1}
       set(v){}
    }

    public func a(){}  
    public func a(x: Int64, y: Int64, z: Int64){}  
}

enum B {
    aa

    public mut prop a { 
       get() {1}
       set(v){}
    }

    public func a(){}  
}

main(): Int64 {
    return 0
}

/* SCAN
error: unexpected modifier 'mut' on property declaration in enum body
  ==> enum25_1.cj:12:12:
   | 
9  | enum B {
   | ~~~~ in enum
10 |     a(Int64,Int64,Int64) | b
   | ...
12 |     public mut prop b : Int64 { 
   |            ^^^ ~~~~ on property declaration
   |            |
   |            unexpected modifier
   | 

error: unexpected modifier 'mut' on property declaration in enum body
  ==> enum25_1.cj:24:12:
   | 
21 | enum B {
   | ~~~~ in enum
22 |     aa
   | ...
24 |     public mut prop a { 
   |            ^^^ ~~~~ on property declaration
   |            |
   |            unexpected modifier
   | 

error: expected ':', found '{'
  ==> enum25_1.cj:24:23:
   | 
24 |     public mut prop a { 
   |                       ^ expected ':' here
   | 
   # note: property declaration must declare type explicitly

3 errors generated, 3 errors printed.
*/
