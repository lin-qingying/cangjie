// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A <: Resource {
    public func isClosed() {
        print("A!");
        true
    }
    public func close() {}
}

const a: Unit = <!EXPECT_CONST!>try (a: A = A()) {
    0
}<!>

main() {
    print("success!\n")
    return a
}
