// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S {
    mut func f() {}
    operator func +(_: Int64) {
        S()
    }
}

main() {
    var a = S()
    (S() + 1).<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
