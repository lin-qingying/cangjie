// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2<T> <: I1<T> {}

open class C1<T> <: I1<T> {
    public func i1(a: Int32): Unit {}
    public func i1(a: T): Unit {}
}

interface I3 <: I2<Int32> {}

class C2 <: C1<Int32> {}

main() {
    var a = C1<Int32>()
    return 0
}

/* SCAN
error: generic instantiation 'I2<Int32>' causes ambiguous function 'i1'
  ==> main2-4.cj:21:17:
   | 
21 | interface I3 <: I2<Int32> {}
   |                 ^^^^^^^^^ 
   | 
note: found candidate
  ==> main2-4.cj:10:10:
   | 
10 |     func i1(a: Int32): Unit
   |          ^^ 
   | 
note: found candidate
  ==> main2-4.cj:11:10:
   | 
11 |     func i1(a: T): Unit
   |          ^^ 
   | 

error: generic instantiation 'C1<Int32>' causes ambiguous function 'i1'
  ==> main2-4.cj:23:13:
   | 
23 | class C2 <: C1<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main2-4.cj:17:17:
   | 
17 |     public func i1(a: Int32): Unit {}
   |                 ^^ 
   | 
note: found candidate
  ==> main2-4.cj:18:17:
   | 
18 |     public func i1(a: T): Unit {}
   |                 ^^ 
   | 

error: generic instantiation 'C1<Int32>' causes ambiguous function 'i1'
  ==> main2-4.cj:26:13:
   | 
26 |     var a = C1<Int32>()
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main2-4.cj:17:17:
   | 
17 |     public func i1(a: Int32): Unit {}
   |                 ^^ 
   | 
note: found candidate
  ==> main2-4.cj:18:17:
   | 
18 |     public func i1(a: T): Unit {}
   |                 ^^ 
   | 

3 errors generated, 3 errors printed.
*/
