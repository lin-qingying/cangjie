// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum e<T> {
    | A(Int32)
    | B(Int32, Int32)

    public func foo(a: T) {
        1
    }
}

extend<T> e<T> {
    public func foo(a: Int32) {
        0
    }
}

main() {
    let a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>e<Int32><!>.A(1)
    return 0
}
