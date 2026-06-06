// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static func f() {
        <!sema_static_function_cannot_access_non_static_member!>g<!>()
    }

    func g(_: Array<Int64>) {}
}

main() {
    A.f()
}
