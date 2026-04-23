// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>public static var ctx: Int64<!>

    static init() {
        let l1 = {=> println(ctx)}
        let l2 = {=> ctx = 10}
        l1()
    }
}

main() {}
