// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class C<T> {
    public static func c1(a: Int32): Int32 {
        0
    }
    public static func c1(a: T): Int32 {
        0
    }
}

class C1 <: C<Int32> {}

class C2 <: C<Int32> {}

main() {
    return 0
}

/* SCAN
error: generic instantiation 'C<Int32>' causes ambiguous function 'c1'
  ==> main3-1.cj:18:13:
   | 
18 | class C1 <: C<Int32> {}
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> main3-1.cj:10:24:
   | 
10 |     public static func c1(a: Int32): Int32 {
   |                        ^^ 
   | 
note: found candidate
  ==> main3-1.cj:13:24:
   | 
13 |     public static func c1(a: T): Int32 {
   |                        ^^ 
   | 

error: generic instantiation 'C<Int32>' causes ambiguous function 'c1'
  ==> main3-1.cj:20:13:
   | 
20 | class C2 <: C<Int32> {}
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> main3-1.cj:10:24:
   | 
10 |     public static func c1(a: Int32): Int32 {
   |                        ^^ 
   | 
note: found candidate
  ==> main3-1.cj:13:24:
   | 
13 |     public static func c1(a: T): Int32 {
   |                        ^^ 
   | 

2 errors generated, 2 errors printed.
*/
