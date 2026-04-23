// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

extend<T> A<T> {
    public func foo() {}
}

main() {
    let a: A<Int32> = A<Int32>()
    a.foo()
    0
}
