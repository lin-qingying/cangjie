// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f



class A {
    public A (var a:Int64 = 3, var b:Int64 =this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

class B {
    public var a :Int64 = 2
    public B (a:Int64 = 3, var b:Int64 =this.a) {
     var d: Int64 =a
     this.b = d + 1
   }
}

open class Parent {
   public var b :Int64 = 3
    public Parent(a:String = "1", var b :Int64 = 3) {
     var c: Int64 = 10
     this.b = 10
   }
}

class C {
    public C (var a:Int64 = 3,  b:Int64 ) {
     var d: Int64 = a
   }
}

open class D<X,Y> {
    public D<T>( x: Int64, y: X, public var z: Y) {}
}

class E {
     public E (var a:Int64 = 3, var b:Int64 = 4 ) { }
     public init (c:Int64 = 3,  d:Int64 = 4 ) {
        this.a = c
        this.b = d
     }
}

class F {
    public F (static var a:Int64) { 
   }
}

class G {
    public G (static private var a:Int64) { 
   }
}

main():Int64{
    return 0
}

/* SCAN
error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:12:27:
   | 
12 |     public A (var a:Int64 = 3, var b:Int64 =this.a) {
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:12:44:
   | 
12 |     public A (var a:Int64 = 3, var b:Int64 =this.a) {
   |                                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:20:23:
   | 
20 |     public B (a:Int64 = 3, var b:Int64 =this.a) {
   |                       ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:20:40:
   | 
20 |     public B (a:Int64 = 3, var b:Int64 =this.a) {
   |                                        ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:28:28:
   | 
28 |     public Parent(a:String = "1", var b :Int64 = 3) {
   |                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:28:48:
   | 
28 |     public Parent(a:String = "1", var b :Int64 = 3) {
   |                                                ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:35:27:
   | 
35 |     public C (var a:Int64 = 3,  b:Int64 ) {
   |                           ^ expected ',' or ')' here
   | 

error: regular parameters must come before member variable parameters
  ==> primaryConstructor5.cj:35:33:
   | 
35 |     public C (var a:Int64 = 3,  b:Int64 ) {
   |               ~~~~~~~~~~~~~~~   ^^^^^^^ unexpected parameter here
   |               |
   |               because it must come before this member variable parameter
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:45:28:
   | 
45 |      public E (var a:Int64 = 3, var b:Int64 = 4 ) { }
   |                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:45:45:
   | 
45 |      public E (var a:Int64 = 3, var b:Int64 = 4 ) { }
   |                                             ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:46:27:
   | 
46 |      public init (c:Int64 = 3,  d:Int64 = 4 ) {
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor5.cj:46:41:
   | 
46 |      public init (c:Int64 = 3,  d:Int64 = 4 ) {
   |                                         ^ expected ',' or ')' here
   | 

error: unexpected modifier 'static' on variable declaration in primary constructor
  ==> primaryConstructor5.cj:53:15:
   | 
53 |     public F (static var a:Int64) { 
   |            ~  ^^^^^^ ~~~ on variable declaration
   |            |  |
   |            |  unexpected modifier
   |            |
   |            in primary constructor
   | 

error: unexpected modifier 'static' on variable declaration in primary constructor
  ==> primaryConstructor5.cj:58:15:
   | 
58 |     public G (static private var a:Int64) { 
   |            ~  ^^^^^^         ~~~ on variable declaration
   |            |  |
   |            |  unexpected modifier
   |            |
   |            in primary constructor
   | 

14 errors generated, 14 errors printed.
*/
