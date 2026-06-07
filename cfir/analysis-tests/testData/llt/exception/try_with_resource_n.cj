// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {}

main() {
    try (<!TYPE_MISMATCH!>input = A()<!>) {
    } catch (e: Exception) {
        return 1
    }
    try (<!TYPE_MISMATCH!>aaa = ()<!>) {
        aaa;
        ()
    }
    return 0
}
