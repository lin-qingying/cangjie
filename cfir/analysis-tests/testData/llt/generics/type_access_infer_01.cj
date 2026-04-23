// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {
    static var c = 5
}

main() {
    try {
        <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>.c >>= 0
    } catch (d: NoneValueException) {}
}
