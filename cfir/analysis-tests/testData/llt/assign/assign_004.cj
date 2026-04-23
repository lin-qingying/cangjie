// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var a = [-1, 1, 2]
    a = <!TYPE_MISMATCH!>1000<!>
    return 0
}

var a = [-1, 1, 2]
var b = 1000

func foo() {
    var a = [-1, 1, 2]
    a = <!TYPE_MISMATCH!>1000<!>
}
