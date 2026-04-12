// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Base {
    public func f() {
        return 1
    }
}

class Data <: Base {
    public static let f = 2
}

main(): Int64 {
    var res = Data.f
    return 0
}

/* SCAN
error: static member 'f' cannot have the same name with non-static member in parent class or interfaces
  ==> class_shadow2.cj:16:5:
   | 
16 |     public static let f = 2
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
