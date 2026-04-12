// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class A<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

interface B<T> {
    func foo(a: Int32): Unit
    func foo(a: T): Unit
}

struct C<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

main() {
    var a = A<Int32>()
    var b: B<Int32>
    var c = C<Int32>()
    return 0
}

/* SCAN
error: generic instantiation 'A<Int32>' causes ambiguous function 'foo'
  ==> inst_dup_functions001.cj:25:13:
   | 
25 |     var a = A<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:10:17:
   | 
10 |     public func foo(a: Int32) {}
   |                 ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:11:17:
   | 
11 |     public func foo(a: T) {}
   |                 ^^^ 
   | 

error: generic instantiation 'B<Int32>' causes ambiguous function 'foo'
  ==> inst_dup_functions001.cj:26:12:
   | 
26 |     var b: B<Int32>
   |            ^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:15:10:
   | 
15 |     func foo(a: Int32): Unit
   |          ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:16:10:
   | 
16 |     func foo(a: T): Unit
   |          ^^^ 
   | 

error: generic instantiation 'C<Int32>' causes ambiguous function 'foo'
  ==> inst_dup_functions001.cj:27:13:
   | 
27 |     var c = C<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:20:17:
   | 
20 |     public func foo(a: Int32) {}
   |                 ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions001.cj:21:17:
   | 
21 |     public func foo(a: T) {}
   |                 ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
