// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj 2>&1 | compare %f

class A<T> {}

interface I<T>{}

extend<T> A<T> <: I<T>{
  static public func me<class A<T> {}

extend<T> A<T> <: I<T>{
  static public func me<T>() {
  }
}

main(): Int64 {}

/* SCAN
error: expected a generic type name after '<' in generic, found keyword 'class'
  ==> extend_quest_ty.cj:14:25:
   | 
14 |   static public func me<class A<T> {}
   |                        ~^^^^^ expected a generic type name here
   |                        |
   |                        after '<' in generic
   | 
help: you could escape keyword as a generic type name using '`':
   | 
14 |   static public func me<`class` A<T> {}
   |                         ~~~~~~~ 
   | 

error: unexpected extend declaration in extend body
  ==> extend_quest_ty.cj:16:1:
   | 
13 | extend<T> A<T> <: I<T>{
   | ~~~~~~ in extend
14 |   static public func me<class A<T> {}
   | ...
16 | extend<T> A<T> <: I<T>{
   | ^^^^^^ unexpected extend declaration
   | 

error: unexpected main function in extend body
  ==> extend_quest_ty.cj:21:1:
   | 
13 | extend<T> A<T> <: I<T>{
   | ~~~~~~ in extend
14 |   static public func me<class A<T> {}
   | ...
21 | main(): Int64 {}
   | ^^^^ unexpected main function
   | 

error: unclosed delimiter: '{'
  ==> extend_quest_ty.cj:21:17:
   | 
13 | extend<T> A<T> <: I<T>{
   |                       ~ to match this opening '{'
14 |   static public func me<class A<T> {}
   | ...
21 | main(): Int64 {}
   |                 ^ expected '}' here
   | 

4 errors generated, 4 errors printed.
*/
