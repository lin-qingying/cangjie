// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class C {
    public operator func +(a: UInt8) {
        print("+")
    }
}

class Cls {
    public var c: C = C()
}

main() {
    var cls = Cls()
    <!sema_type_incompatible!>c<!>ls.c += 2
    return 0
}
