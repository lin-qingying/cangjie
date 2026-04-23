// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(a!: Int32 = 1, b!: Int32 = 1) {
    return a + b
}

main() {
    var x = foo
    var res = x(<!NAMED_ARGUMENTS_NOT_ALLOWED!>a: 2<!>, <!NAMED_ARGUMENTS_NOT_ALLOWED!>b: 2<!>)
    if (res != 4) {
        return 1
    }
    return 0
}
