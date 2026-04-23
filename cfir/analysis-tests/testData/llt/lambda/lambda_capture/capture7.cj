// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var a: Int32 = 1

    func foo() {
        return a
    }

    var ret = if (true) {
        <!USE_FUNC_CAPTURE_VAR_ALONE!>foo<!>
    } else {
        <!USE_FUNC_CAPTURE_VAR_ALONE!>foo<!>
    }

    if (ret() != 1) {
        return 1
    }

    return 0
}
