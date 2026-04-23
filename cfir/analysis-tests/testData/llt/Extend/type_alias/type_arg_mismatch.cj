// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    func f(): Int64
}

class C<R> {}

type c<T> = C<T>

extend <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>c<!> {
    public func f1() {
        2
    }
}

main() {
    return 0
}
