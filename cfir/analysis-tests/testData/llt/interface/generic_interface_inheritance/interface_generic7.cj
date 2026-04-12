// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f %error_count_limit_all %noColor 2>&1 | compare %f

interface I<T> {}

interface II<T> <: I<T> & I<Int32> & I<Float64> {}

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
  ==> interface_generic7.cj:15:1:
   | 
15 | interface III2 <: II<Int32> {}
   | ^ 
   | 

error: interface 'III4' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:19:1:
   | 
19 | interface III4 <: II<Float64> {}
   | ^ 
   | 

error: interface 'III5' inherits or implements duplicate interface 'II'
  ==> interface_generic7.cj:21:1:
   | 
21 | interface III5<T> <: II<T> & II<T> {}
   | ^ 
   | 

error: interface 'III10' inherits or implements duplicate interface 'II'
  ==> interface_generic7.cj:31:1:
   | 
31 | interface III10 <: II<Int64> & II<Int64> {}
   | ^ 
   | 

error: interface 'III16' inherits or implements duplicate interface 'II'
  ==> interface_generic7.cj:43:1:
   | 
43 | interface III16<T> <: II<T> & II<Int64> & II<T> {}
   | ^ 
   | 

error: interface 'III18' inherits or implements duplicate interface 'II'
  ==> interface_generic7.cj:47:1:
   | 
47 | interface III18<T> <: II<T> & II<Int64> & II<Int64> {}
   | ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:15:19:
   | 
15 | interface III2 <: II<Int32> {}
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:19:19:
   | 
19 | interface III4 <: II<Float64> {}
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:23:30:
   | 
23 | interface III6<T> <: II<T> & II<Int32> {}
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:27:30:
   | 
27 | interface III8<T> <: II<T> & II<Float64> {}
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:29:31:
   | 
29 | interface III9 <: II<Int64> & II<Int32> {}
   |                               ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:33:32:
   | 
33 | interface III11 <: II<Int64> & II<Float64> {}
   |                                ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:45:43:
   | 
45 | interface III17<T> <: II<T> & II<Int64> & II<Int32> {}
   |                                           ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic7.cj:49:43:
   | 
49 | interface III19<T> <: II<T> & II<Int64> & II<Float64> {}
   |                                           ^ 
   | 

14 errors generated, 14 errors printed.
*/
