// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {}

type a = A

type b = A

extend a {
    public func ggg() {
        2
    }
}

extend b {
    public func h() {
        ggg()
    }
}

main() {
    var x = a()
    if (x.h() != 2) {
        return 1
    }
    return 0
}
