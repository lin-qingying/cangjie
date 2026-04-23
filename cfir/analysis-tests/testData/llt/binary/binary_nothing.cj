// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

var a = 0

open class C1 {
    public open func f(): Bool {
        a = 3
        return true
    }
}

class C2 <: C1 {
    public override func f(): Bool {
        super.f()
    }
}

main(): Int64 {
    if (C2().f()) {
        return (a - 3)
    } else {
        return 2
    }
        % <!UNRESOLVED_REFERENCE!>n<!>
}
