// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class C1 {
    public func foo():Int64 {
        return 0
    }
}

class C2 <: C1 {
    public func foo1():Int64 {
        var f = super<C1>.foo() 
        return 0
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: comparison operators cannot be chained
  ==> class_super_access_invalid_1.cj:17:25:
   | 
17 |         var f = super<C1>.foo() 
   |                      ~  ^ 
   | 

error: expected expression after '>', found '.'
  ==> class_super_access_invalid_1.cj:17:26:
   | 
17 |         var f = super<C1>.foo() 
   |                          ^ expected expression here
   | 

2 errors generated, 2 errors printed.
*/
