// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Base {
    public let val = 1
}

open class Data <: Base {
    public func val() {
        return 0
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: function member 'val' cannot have the same name with variable member in parent class or interfaces
  ==> class_shadow10.cj:14:17:
   | 
14 |     public func val() {
   |                 ^^^ 
   | 
note: found conflict here
  ==> class_shadow10.cj:10:16:
   | 
10 |     public let val = 1
   |                ^^^ 
   | 

1 error generated, 1 error printed.
*/
