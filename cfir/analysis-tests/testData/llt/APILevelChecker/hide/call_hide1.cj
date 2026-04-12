// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt hide_use1.cj -o hide_use1.%middle
// ERRCHECK: %compiler %cmp_opt hide_use1.%middle %n.cj -o %n.%suffix -Woff all


import ohos.dep.*
import ohos.dep

class D<T> <: A/*error*/ where T <: A/*error*/ {
    func xxx<R>(d: A/*error*/): A/*error*/ where R <: A/*error*/ {
        var a: A/*error*/ = A()/*error*/
        B/*error*/.AA
        B/*error*/.BB(1)
        E.AA/*error*/
        E.BB(1)/*error*/
        goo(1)/*error*/
        var b: D<A/*error*/>
        var c: dep.A/*error*/
        a is A/*error*/
        a as A/*error*/
        aaa/*error*/ // hiden global var
        a
    }
}

main() {}

/* SCAN
error: undeclared identifier 'A'
  ==> call_hide1.cj:16:37:
   | 
16 | class D<T> <: A/*error*/ where T <: A/*error*/ {
   |                                     ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:16:15:
   | 
16 | class D<T> <: A/*error*/ where T <: A/*error*/ {
   |               ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:17:55:
   | 
17 |     func xxx<R>(d: A/*error*/): A/*error*/ where R <: A/*error*/ {
   |                                                       ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:17:20:
   | 
17 |     func xxx<R>(d: A/*error*/): A/*error*/ where R <: A/*error*/ {
   |                    ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:17:33:
   | 
17 |     func xxx<R>(d: A/*error*/): A/*error*/ where R <: A/*error*/ {
   |                                 ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:18:16:
   | 
18 |         var a: A/*error*/ = A()/*error*/
   |                ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:18:29:
   | 
18 |         var a: A/*error*/ = A()/*error*/
   |                             ^^^ 
   | 

error: undeclared identifier 'B'
  ==> call_hide1.cj:19:9:
   | 
19 |         B/*error*/.AA
   |         ^^^^^^^^^^^^^ 
   | 

error: undeclared identifier 'B'
  ==> call_hide1.cj:20:9:
   | 
20 |         B/*error*/.BB(1)
   |         ^^^^^^^^^^^^^^^^ 
   | 

error: undeclared identifier 'AA'
  ==> call_hide1.cj:21:9:
   | 
21 |         E.AA/*error*/
   |         ^^^^ 
   | 

error: undeclared identifier 'BB'
  ==> call_hide1.cj:22:9:
   | 
22 |         E.BB(1)/*error*/
   |         ^^^^^^^ 
   | 

error: undeclared identifier 'goo'
  ==> call_hide1.cj:23:9:
   | 
23 |         goo(1)/*error*/
   |         ^^^^^^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:24:18:
   | 
24 |         var b: D<A/*error*/>
   |                  ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:25:16:
   | 
25 |         var c: dep.A/*error*/
   |                ^^^^^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:26:14:
   | 
26 |         a is A/*error*/
   |              ^ 
   | 

error: undeclared identifier 'A'
  ==> call_hide1.cj:27:14:
   | 
27 |         a as A/*error*/
   |              ^ 
   | 

error: undeclared identifier 'aaa'
  ==> call_hide1.cj:28:9:
   | 
28 |         aaa/*error*/ // hiden global var
   |         ^^^ 
   | 

17 errors generated, 17 errors printed.
*/
