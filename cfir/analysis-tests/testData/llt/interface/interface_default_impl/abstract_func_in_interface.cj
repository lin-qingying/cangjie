// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f1<T>(<!chir_dce_unused_variable!>a<!>: T): Unit {} // ok
    static func f2<T>(a: T): Unit // ok
    func f3<T>(<!chir_dce_unused_variable!>a<!>: T): Unit {} // error
    func f4<T>(a: T): Unit // error
}

main() {
    return 0
}
