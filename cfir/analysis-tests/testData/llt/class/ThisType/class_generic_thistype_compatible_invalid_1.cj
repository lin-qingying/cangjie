// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class C1<T> {
    public open func foo(): This {
        return this
    }
}

class C2<T> <: C1<T> {
    public override func <!INHERIT_NOT_RETURN_THIS!>foo<!>(): C2<T> {
        return this
    }
}

main() {
    return 1
}
