// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

var a1 = {
    =>
    var a: Int32 = 1
    let b: Int32 = 2
    if (a == 2) {
        return <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>
    } else {
        return foo(<!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>)
    }
    if (a == 2) {
        return {=> b}
    } else {
        return foo({=> b})
    }
}
var a2 = {
    =>
    var a: Int32 = 1
    if (a == 2) {
        return <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>
    } else {
        return foo(<!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>)
    }
}

func foo(x: () -> Int32) {
    return x
}
