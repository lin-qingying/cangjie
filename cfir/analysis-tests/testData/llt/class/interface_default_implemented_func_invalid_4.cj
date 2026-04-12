// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface B<T> {
    func foo(x: Int32): Int64 {
        return 1
    }
}

class C<T> <: B<T> {
    public func foo(x: T): Float64 {
        return .2
    }
}

func test<T>() {
    var a = C<T>()
}

main(): Int64 {
    var a = test<Int32>()
    return 0
}

/* SCAN
error: generic instantiation 'test<Int32>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_4.cj:26:13:
   | 
26 |     var a = test<Int32>()
   |             ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_4.cj:10:10:
   | 
10 |     func foo(x: Int32): Int64 {
   |          ^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_4.cj:16:17:
   | 
16 |     public func foo(x: T): Float64 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
