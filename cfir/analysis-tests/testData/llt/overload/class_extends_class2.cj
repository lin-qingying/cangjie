// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

abstract class Base {
    public static func foo(a: Int64) {
        return a
    }
}

class Data <: Base {
    public func foo() {
        return 1
    }
}

main(): Int64 {
    0
}

/* SCAN
error: non-static member 'foo' cannot have the same name with static member in parent class or interfaces
  ==> class_extends_class2.cj:16:5:
   | 
16 |     public func foo() {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
