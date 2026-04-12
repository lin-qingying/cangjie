// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

func test1(a: Int32): Int32 {
    a
}

func test2(a: T): T {
    a
}

func test3<T>(a: T): T {
    a
}

main() {
    var a = test1()
    var b = test1<Int32>()
    var c = test1(1)
    var d = test2()
    var e = test2<Int32>()
    var f = test2(1)
    var g = test2<Int32>(1)
    var h = test3(1)
    var i = test3<Int32>()
    var j = test3()
    var k = test3<Int32, Int32>(1)
    var l = test3<Int32>(1)
    return 0
}

/* SCAN
error: undeclared type name 'T'
  ==> function14_test.cj:13:15:
   | 
13 | func test2(a: T): T {
   |               ^ 
   | 

error: undeclared type name 'T'
  ==> function14_test.cj:13:19:
   | 
13 | func test2(a: T): T {
   |                   ^ 
   | 

error: missing argument for parameter list '(Int32)' in call
  ==> function14_test.cj:22:18:
   | 
22 |     var a = test1()
   |                  ^^ expected 1 argument, found 0
   | 
note: found candidate
 ==> function14_test.cj:9:6:
  | 
9 | func test1(a: Int32): Int32 {
  |      ^^^^^ 
  | 

error: type argument's number does not match type parameter's number
  ==> function14_test.cj:23:13:
   | 
23 |     var b = test1<Int32>()
   |             ^^^^^^^^^^^^ 
   | 

error: missing argument for parameter list '(Invalid)' in call
  ==> function14_test.cj:25:18:
   | 
25 |     var d = test2()
   |                  ^^ expected 1 argument, found 0
   | 
note: found candidate
  ==> function14_test.cj:13:6:
   | 
13 | func test2(a: T): T {
   |      ^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> function14_test.cj:26:13:
   | 
26 |     var e = test2<Int32>()
   |             ^^^^^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> function14_test.cj:28:13:
   | 
28 |     var g = test2<Int32>(1)
   |             ^^^^^^^^^^^^ 
   | 

error: missing argument for parameter list '(Generics-T)' in call
  ==> function14_test.cj:30:25:
   | 
30 |     var i = test3<Int32>()
   |                         ^^ expected 1 argument, found 0
   | 
note: found candidate
  ==> function14_test.cj:17:6:
   | 
17 | func test3<T>(a: T): T {
   |      ^^^^^ 
   | 

error: missing argument for parameter list '(Generics-T)' in call
  ==> function14_test.cj:31:18:
   | 
31 |     var j = test3()
   |                  ^^ expected 1 argument, found 0
   | 
note: found candidate
  ==> function14_test.cj:17:6:
   | 
17 | func test3<T>(a: T): T {
   |      ^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> function14_test.cj:32:13:
   | 
32 |     var k = test3<Int32, Int32>(1)
   |             ^^^^^^^^^^^^^^^^^^^ 
   | 

10 errors generated, 10 errors printed.
*/
