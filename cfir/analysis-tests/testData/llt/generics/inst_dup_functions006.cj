// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class A<T, U> {
    public func test(a: Int64) {
        return 1
    }
    public func test(a: T) {
        return 2
    }
}

class B<T> <: A<T, Bool> {
    public var a: T
    public init(input: T) {
        a = input
    }

    public func test(a: Int32) {
        return 3
    }
    public func i() {
        var j = this.test(a)
        return j
    }
}

main(): Int64 {
    var res1 = B<Int32>(1).i()
    var res2 = B<Int64>(1).i()
    return 0
}

/* SCAN
error: generic instantiation 'B<Int32>' causes ambiguous function 'test'
  ==> inst_dup_functions006.cj:34:16:
   | 
34 |     var res1 = B<Int32>(1).i()
   |                ^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions006.cj:13:17:
   | 
13 |     public func test(a: T) {
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions006.cj:24:17:
   | 
24 |     public func test(a: Int32) {
   |                 ^^^^ 
   | 

error: generic instantiation 'B<Int64>' causes ambiguous function 'test'
  ==> inst_dup_functions006.cj:35:16:
   | 
35 |     var res2 = B<Int64>(1).i()
   |                ^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions006.cj:10:17:
   | 
10 |     public func test(a: Int64) {
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions006.cj:13:17:
   | 
13 |     public func test(a: T) {
   |                 ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
