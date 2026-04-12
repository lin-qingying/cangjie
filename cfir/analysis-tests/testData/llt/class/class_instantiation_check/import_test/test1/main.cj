// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt pkga.%middle %f -o %n.%suffix 2>&1 | compare %f

import pkga.I1

interface I2 <: I1<Int32> {}

interface I3<T> <: I1<T> {}

interface I4 <: I3<Int32> {}

class C1 <: I1<Int32> {}

open class C2<T> <: I1<T> {}

class C3 <: I3<Int32> {}

class C4 <: C2<Int32> {}

main() {
    var a = C2<Int32>()
    return 0
}

/* SCAN
error: class 'C1' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main.cj:19:1:
   | 
19 | class C1 <: I1<Int32> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> (package pkga)pkga.cj:11:5:

error: class 'C2' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main.cj:21:1:
   | 
21 | open class C2<T> <: I1<T> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> (package pkga)pkga.cj:10:5:
note: unimplemented interface function 'i1'
  ==> (package pkga)pkga.cj:11:5:

error: class 'C3' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main.cj:23:1:
   | 
23 | class C3 <: I3<Int32> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> (package pkga)pkga.cj:11:5:

error: class 'C4' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main.cj:25:1:
   | 
25 | class C4 <: C2<Int32> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> (package pkga)pkga.cj:11:5:

error: generic instantiation 'I1<Int32>' causes ambiguous function 'i1'
  ==> main.cj:13:17:
   | 
13 | interface I2 <: I1<Int32> {}
   |                 ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

error: generic instantiation 'I3<Int32>' causes ambiguous function 'i1'
  ==> main.cj:17:17:
   | 
17 | interface I4 <: I3<Int32> {}
   |                 ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

error: generic instantiation 'I1<Int32>' causes ambiguous function 'i1'
  ==> main.cj:19:13:
   | 
19 | class C1 <: I1<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

error: generic instantiation 'I3<Int32>' causes ambiguous function 'i1'
  ==> main.cj:23:13:
   | 
23 | class C3 <: I3<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

error: generic instantiation 'C2<Int32>' causes ambiguous function 'i1'
  ==> main.cj:25:13:
   | 
25 | class C4 <: C2<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

error: generic instantiation 'C2<Int32>' causes ambiguous function 'i1'
  ==> main.cj:28:13:
   | 
28 |     var a = C2<Int32>()
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:10:
note: found candidate
  ==> (package pkga)pkga.cj:11:10:

10 errors generated, 10 errors printed.
*/
