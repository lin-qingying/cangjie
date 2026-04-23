// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {
    func foo(a: T): Int64
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<T, V><!> <: I<T> & I<V> {}

main() {
    let c = C<Int64, String>()
    return 0
}
