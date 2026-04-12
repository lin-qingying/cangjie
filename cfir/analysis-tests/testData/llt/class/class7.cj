// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    protected static var a1: Int64 = 1
    private static var a2: Int64 = 2
    private class A1 {  
        public var a3 = a1
        public var a4 = a2
    }
    protected interface I1{} 
}

class B <: A {
    public var b: A1 = A1()
    public class B1 { 
        public class B2 { 
            public var b1 = a1
            public var b2 = a2
            public var b3 : I1
        }
    }
}

/* SCAN
[33mwarning[0m: '--error-count-limit' is specified multiple times. The last one is chosen.
[33mwarning[0m: '--diagnostic-format' is specified multiple times. The last one is chosen.
error: unexpected class declaration in class body
  ==> class7.cj:12:13:
   | 
9  | open class A {
   |      ~~~~~ in class
10 |     protected static var a1: Int64 = 1
   | ...
12 |     private class A1 {  
   |             ^^^^^ unexpected class declaration
   | 

error: unexpected interface declaration in class body
  ==> class7.cj:16:15:
   | 
9  | open class A {
   |      ~~~~~ in class
10 |     protected static var a1: Int64 = 1
   | ...
16 |     protected interface I1{} 
   |               ^^^^^^^^^ unexpected interface declaration
   | 

error: unexpected class declaration in class body
  ==> class7.cj:21:12:
   | 
19 | class B <: A {
   | ~~~~~ in class
20 |     public var b: A1 = A1()
21 |     public class B1 { 
   |            ^^^^^ unexpected class declaration
   | 

error: unexpected class declaration in class body
  ==> class7.cj:22:16:
   | 
21 |     public class B1 { 
   |            ~~~~~ in class
22 |         public class B2 { 
   |                ^^^^^ unexpected class declaration
   | 

4 errors generated, 4 errors printed.
*/
