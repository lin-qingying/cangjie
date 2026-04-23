// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Test {
    func f(x: Int64): This <!TYPE_MISMATCH!>{
        (match (x) {
            case _ => this
        }).g()
    }<!>

    func g(): This <!TYPE_MISMATCH!>{
        (match {
            case _ => this
        }).g()
    }<!>
}
