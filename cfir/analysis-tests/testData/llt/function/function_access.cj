// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
class A {
    public var value = "default"
    private init() {}
    public init(value!: String = "") {
        this.value = value
    }
    public func f(a: Int64, b!: Int32 = 2): Int64 {
        return 1;
    }
    private func f(a: Int64, b!: Int64): Int64 {
        a
    }
}

main() {
    var a = A()
    if (a.value != "") {
        return 1
    }
    var b = a.f(3, b: 2)
    if (b != 1) {
        return 2
    }
    return 0
}
