// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C {
    public func f() {
        1
    }
    private func f(a: Int64) {
        2
    }
}

extend C {
    private func f(a: Int64) {
        3
    }
    func g1() {
        f()
    }
    func g2() {
        f(1)
    }
}

main() {
    var c = C()
    if (c.g1() != 1) {
        return 1
    }
    if (c.g2() != 3) {
        return 2
    }
    return 0
}
