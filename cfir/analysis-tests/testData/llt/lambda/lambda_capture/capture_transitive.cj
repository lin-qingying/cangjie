// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var a = 1
    func f() {
        return a
    }
    func g() {
        return f() + 1
    }
    {a: Int64 => <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>}
    return 0
}
