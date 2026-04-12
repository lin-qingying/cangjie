// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public A (var a:Int64 = 3, var b:Int64 =this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

struct B {
    public var a :Int64 = 2
    public B (a:Int64 = 3, var b:Int64 =this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

struct Parent {
   public var b :Int64 = 3
    public Parent(a:String = "1", var b :Int64 = 3) {
     var c: Int64 = 10
     this.b = 10
   }
}

struct C {
    public C (var a!:Int64 = 3,  b:Int64 ) { 
     var d: Int64 =a
   }
}

struct B<X,Y> {
    public B<T>( x: Int64, y: X, public var z: Y) {}
    public init<T>() {}
}

main():Int64{
    return 0
}

/* SCAN
error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:10:27:
   | 
10 |     public A (var a:Int64 = 3, var b:Int64 =this.a) {
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:10:44:
   | 
10 |     public A (var a:Int64 = 3, var b:Int64 =this.a) {
   |                                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:18:23:
   | 
18 |     public B (a:Int64 = 3, var b:Int64 =this.a) {
   |                       ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:18:40:
   | 
18 |     public B (a:Int64 = 3, var b:Int64 =this.a) {
   |                                        ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:26:28:
   | 
26 |     public Parent(a:String = "1", var b :Int64 = 3) {
   |                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:26:48:
   | 
26 |     public Parent(a:String = "1", var b :Int64 = 3) {
   |                                                ^ expected ',' or ')' here
   | 

error: unnamed parameters must come before named parameters
  ==> primaryConstructor5.cj:33:34:
   | 
33 |     public C (var a!:Int64 = 3,  b:Int64 ) { 
   |               ~~~~~~~~~~~~~~~~   ^^^^^^^ unexpected unnamed parameter here
   |               |
   |               because it must come before this named parameter
   | 

7 errors generated, 7 errors printed.
*/
