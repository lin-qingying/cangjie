// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    var a: Int64 = 1
    static var b: Int64 = 1
    static init() {
        println(<!sema_static_function_cannot_access_non_static_member!>a<!>)
    }
}

main() {
    return 0
}
