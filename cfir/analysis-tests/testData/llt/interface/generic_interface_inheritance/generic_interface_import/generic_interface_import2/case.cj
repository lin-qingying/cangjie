// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/pkg1.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1.%middle pkg2/pkg2.cj -o pkg2.%middle
// EXEC: %compiler %cmp_opt pkg1.%middle pkg2.%middle %f -o %n.%suffix 2>&1 | compare %f

internal import pkg1.I
internal import pkg2.II

interface III1<T> <: II<T> {}

interface III2 <: II<Int32> {}

interface III3 <: II<Int64> {}

interface III4 <: II<Float64> {}

interface III5<T> <: II<T> & II<T> {}

interface III6<T> <: II<T> & II<Int32> {}

interface III7<T> <: II<T> & II<Int64> {}

interface III8<T> <: II<T> & II<Float64> {}

interface III9 <: II<Int64> & II<Int32> {}

interface III10 <: II<Int64> & II<Int64> {}

interface III11 <: II<Int64> & II<Float64> {}

interface III12<T> <: II<T> & II<Int64> & I<T> {}

interface III13<T> <: II<T> & II<Int64> & I<Int32> {}

interface III14<T> <: II<T> & II<Int64> & I<Int64> {}

interface III15<T> <: II<T> & II<Int64> & I<Float64> {}

interface III16<T> <: II<T> & II<Int64> & II<T> {}

interface III17<T> <: II<T> & II<Int64> & II<Int32> {}

interface III18<T> <: II<T> & II<Int64> & II<Int64> {}

interface III19<T> <: II<T> & II<Int64> & II<Float64> {}

main() {
    return 0
}

/* SCAN
error: interface 'III2' inherits or implements duplicate interface 'I'
  ==> case.cj:17:1:
   | 
17 | interface III2 <: II<Int32> {}
   | ^ 
   | 

error: interface 'III4' inherits or implements duplicate interface 'I'
  ==> case.cj:21:1:
   | 
21 | interface III4 <: II<Float64> {}
   | ^ 
   | 

error: interface 'III5' inherits or implements duplicate interface 'II'
  ==> case.cj:23:1:
   | 
23 | interface III5<T> <: II<T> & II<T> {}
   | ^ 
   | 

error: interface 'III10' inherits or implements duplicate interface 'II'
  ==> case.cj:33:1:
   | 
33 | interface III10 <: II<Int64> & II<Int64> {}
   | ^ 
   | 

error: interface 'III16' inherits or implements duplicate interface 'II'
  ==> case.cj:45:1:
   | 
45 | interface III16<T> <: II<T> & II<Int64> & II<T> {}
   | ^ 
   | 

error: interface 'III18' inherits or implements duplicate interface 'II'
  ==> case.cj:49:1:
   | 
49 | interface III18<T> <: II<T> & II<Int64> & II<Int64> {}
   | ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:17:19:
   | 
17 | interface III2 <: II<Int32> {}
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:21:19:
   | 
21 | interface III4 <: II<Float64> {}
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:25:30:
   | 
25 | interface III6<T> <: II<T> & II<Int32> {}
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:29:30:
   | 
29 | interface III8<T> <: II<T> & II<Float64> {}
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:31:31:
   | 
31 | interface III9 <: II<Int64> & II<Int32> {}
   |                               ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:35:32:
   | 
35 | interface III11 <: II<Int64> & II<Float64> {}
   |                                ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:47:43:
   | 
47 | interface III17<T> <: II<T> & II<Int64> & II<Int32> {}
   |                                           ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> case.cj:51:43:
   | 
51 | interface III19<T> <: II<T> & II<Int64> & II<Float64> {}
   |                                           ^ 
   | 

14 errors generated, 14 errors printed.
*/
