// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>v<!>: Int64
    static init() {
        for (i in 1..3) {
            v = i
            if (i == 2) {
                break
            }
        }
    }
}

main() {
    if (A.v == 2) {
        return 0
    }
    return 1
}
