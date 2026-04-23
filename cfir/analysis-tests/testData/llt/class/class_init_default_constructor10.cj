// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Tree {
    var left: Tree = Tree_None()
    var right: Tree = Tree_None()
    init(left: Tree, right: Tree) {
        this.left = left
        this.right = right
    }
}

<!EXPLICIT_SUPER_CALL_REQUIRED!>class Tree_None <: Tree {}<!>
