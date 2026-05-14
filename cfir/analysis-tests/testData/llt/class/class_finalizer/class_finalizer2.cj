// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public var x = 1
    ~init() {
        f(<!INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER!>this<!>)
        let a = {=> this.x}
        {=> x}
        <!INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER!>this<!>
        func g() {
            x
        }
    }
}

func f(a: A) {
    return a
}

main() {
    0
}
