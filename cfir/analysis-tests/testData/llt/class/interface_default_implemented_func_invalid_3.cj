// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 <: I1 {}

extend R1 {
    public func foo1(): Int64 {
        return 0
    }
}

main() {
    return -1
}

/* SCAN
error: extend member 'foo1' is not allowed to shadow members of 'Struct-R1'
  ==> interface_default_implemented_func_invalid_3.cj:18:17:
   | 
18 |     public func foo1(): Int64 {
   |                 ^^^^ 
   | 
note: shadowed definition of 'foo1' is here
  ==> interface_default_implemented_func_invalid_3.cj:10:10:
   | 
10 |     func foo1(): Int64 {
   |          ^^^^ 
   | 

1 error generated, 1 error printed.
*/
