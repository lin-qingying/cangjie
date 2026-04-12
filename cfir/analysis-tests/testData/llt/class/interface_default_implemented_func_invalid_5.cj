// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    let x: Int32 = 1
    var a = C(x)
    return 0
}

interface B<T> {
    func foo(x: Int32): Int64 {
        return 1
    }
}

class C<T> <: B<T> {
    init(a: T) {}
    public func foo(x: T): Float64 {
        return .2
    }
}

/* SCAN
error: generic instantiation 'C<Int32>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_5.cj:11:13:
   | 
11 |     var a = C(x)
   |             ^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_5.cj:16:10:
   | 
16 |     func foo(x: Int32): Int64 {
   |          ^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_5.cj:23:17:
   | 
23 |     public func foo(x: T): Float64 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
