// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E<T0, T1> {
    EE
}

class A<T> {
    static var a = 1
}

main() {
    A<Option<<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>E<!>>>.a <<= 2
    return 1
}
