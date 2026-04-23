// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg

import pkg.*

func bar(): Any {
    let a = B()
    return a
}

main() {
    var res = bar()
    if (res is B) {
        0
    } else {
        1
    }
}
