// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    func f() {
        var x = 1
        func g() {
            x
        }
        <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    }
    {
        x: Int64, y: Int64 =>
        var a = x + y
        <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> 2 * a}<!>
    }
    return 0
}
