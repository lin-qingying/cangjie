// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class Tree {
    var left: Tree = Tree_None()
    var right: Tree = Tree_None()
    init(left: Tree, right: Tree) {
        this.left = left
        this.right = right
    }
}

class Tree_None <: Tree {}

/* SCAN
error: there is no non-parameter constructor in super class, please invoke super call explicitly
  ==> class_init_default_constructor10.cj:18:7:
   | 
18 | class Tree_None <: Tree {}
   |       ^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
