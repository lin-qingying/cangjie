// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Parent {
    protected var field: Int32 = 1
}

class Child <: Parent {
    public func main1() {
        var child: Child = Child()
        field = 2
        if (child.field != 1) {
            return 1
        }
        return 0
    }
}

main(a: Int32): Int64 {
    return Child().main1()
}

/* SCAN
error: 'main' cannot be defined with parameter whose type is not 'Array<String>'
  ==> class_main1.cj:24:1:
   | 
24 | main(a: Int32): Int64 {
   | ^ 
   | 

1 error generated, 1 error printed.
*/
