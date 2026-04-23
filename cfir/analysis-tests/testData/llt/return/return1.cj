// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(): Float64 {
    var a: Int64 = 1
    <!TYPE_MISMATCH!>a<!>
}

<!CONFLICTING_OVERLOADS!>func f<!>(): Int64 {
    <!TYPE_MISMATCH!>while (true) {}<!>
}

func foo() {
    return true
}

func bar() {
    return 1
}
