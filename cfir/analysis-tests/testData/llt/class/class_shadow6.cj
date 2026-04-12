// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

open class Base {
    public func f(): Int32 {
        return 1
    }
}

class Data <: Base {
    public static func f(): Int32 {
        return 0
    }
}

main(): Int64 {
    var res = Data.f()
    return 0
}

/* SCAN
error: static member 'f' cannot have the same name with non-static member in parent class or interfaces
  ==> class_shadow6.cj:16:5:
   | 
16 |     public static func f(): Int32 {
   |     ^ 
   | 

error: cannot override function 'f'
  ==> class_shadow6.cj:16:24:
   | 
16 |     public static func f(): Int32 {
   |                        ^ 
   | 
note: the parent function is not modified by 'open'
  ==> class_shadow6.cj:10:17:
   | 
10 |     public func f(): Int32 {
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
