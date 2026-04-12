// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2 <: I1<Int32> {}

interface I3 <: I1<Int64> {}

class C1 <: I1<Int32> {}

class C2 <: I1<Int64> {}

main() {
    return 0
}

/* SCAN
error: class 'C1' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main2-2.cj:18:1:
   | 
18 | class C1 <: I1<Int32> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> main2-2.cj:11:5:
   | 
11 |     func i1(a: T): Unit
   |     ^ 
   | 

error: class 'C2' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main2-2.cj:20:1:
   | 
20 | class C2 <: I1<Int64> {}
   | ^ 
   | 
note: unimplemented interface function 'i1'
  ==> main2-2.cj:10:5:
   | 
10 |     func i1(a: Int32): Unit
   |     ^ 
   | 
note: unimplemented interface function 'i1'
  ==> main2-2.cj:11:5:
   | 
11 |     func i1(a: T): Unit
   |     ^ 
   | 

error: generic instantiation 'I1<Int32>' causes ambiguous function 'i1'
  ==> main2-2.cj:14:17:
   | 
14 | interface I2 <: I1<Int32> {}
   |                 ^^^^^^^^^ 
   | 
note: found candidate
  ==> main2-2.cj:10:10:
   | 
10 |     func i1(a: Int32): Unit
   |          ^^ 
   | 
note: found candidate
  ==> main2-2.cj:11:10:
   | 
11 |     func i1(a: T): Unit
   |          ^^ 
   | 

error: generic instantiation 'I1<Int32>' causes ambiguous function 'i1'
  ==> main2-2.cj:18:13:
   | 
18 | class C1 <: I1<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main2-2.cj:10:10:
   | 
10 |     func i1(a: Int32): Unit
   |          ^^ 
   | 
note: found candidate
  ==> main2-2.cj:11:10:
   | 
11 |     func i1(a: T): Unit
   |          ^^ 
   | 

4 errors generated, 4 errors printed.
*/
