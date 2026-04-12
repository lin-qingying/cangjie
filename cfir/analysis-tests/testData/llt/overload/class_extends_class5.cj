// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

abstract class Base {
    public static func foo(): Int32 {
        return 0
    }
}

class Data <: Base {
    public func foo(): Int32 {
        return 4
    }
}

/* SCAN
error: non-static member 'foo' cannot have the same name with static member in parent class or interfaces
  ==> class_extends_class5.cj:20:5:
   | 
20 |     public func foo(): Int32 {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
