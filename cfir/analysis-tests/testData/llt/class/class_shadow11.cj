// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Base {
    public func val() {
        return 0
    }
}

open class Data <: Base {
    public let val = 1
}

main(): Int64 {
    return 0
}

/* SCAN
error: variable member 'val' cannot have the same name with function member in parent class or interfaces
  ==> class_shadow11.cj:16:16:
   | 
16 |     public let val = 1
   |                ^^^ 
   | 
note: found conflict here
  ==> class_shadow11.cj:10:17:
   | 
10 |     public func val() {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
