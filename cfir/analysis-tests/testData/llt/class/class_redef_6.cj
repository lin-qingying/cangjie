// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public redef func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: 'redef' cannot be used to modify an instance 'function'
  ==> class_redef_6.cj:16:12:
   | 
16 |     public redef func foo(): Int64 {
   |            ^ 
   | 

error: non-static member 'foo' cannot have the same name with static member in parent class or interfaces
  ==> class_redef_6.cj:16:5:
   | 
16 |     public redef func foo(): Int64 {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
