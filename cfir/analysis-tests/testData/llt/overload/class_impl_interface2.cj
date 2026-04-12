// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface Base {
    func foo(a: Int64) {
        return a
    }
}

class Data <: Base {
    public static func foo() {
        return 1
    }
}

/* SCAN
error: static member 'foo' cannot have the same name with non-static member in parent class or interfaces
  ==> class_impl_interface2.cj:16:5:
   | 
16 |     public static func foo() {
   |     ^ 
   | 

error: 'main' is missing
 ==> class_impl_interface2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
