// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    return Int64(Data().foo())
}

abstract class Base {
    private func foo(): Int32
}

class Data <: Base {
    public func foo(): Int32 {
        return 0
    }
}

/* SCAN
error: the visibility of an 'abstract' function must be 'public' or 'protected'
  ==> class_access_control10.cj:14:18:
   | 
14 |     private func foo(): Int32
   |                  ^^^ 
   | 

1 error generated, 1 error printed.
*/
