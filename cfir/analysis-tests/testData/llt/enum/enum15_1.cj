// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum Color {
    Red | Blue | Green
}

class Green {}

var Red: Int64 = 2

func test(Green: Int32) {
    var a: Int32 = Green
    a
}

main() {
    var t0 = <!sema_ref_not_be_type!>G<!>reen
    var t1 = Red
    var t2 = Blue

    return 0
}
