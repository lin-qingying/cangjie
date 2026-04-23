// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    func foo(): This <!TYPE_MISMATCH!>{
        if (1 > 2) {
            this
        } else {
            this
        }
    }<!>

    func foo2(): This {
        <!TYPE_MISMATCH!>(if (1 > 2) {
            this
        } else {
            this
        }).foo()<!>
    }
}
