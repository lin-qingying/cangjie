// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(a: (Int64) -> Int32) {}

main() {
    test({<!PARAM_COUNT_MISMATCH!>i, j<!> => <!TYPE_MISMATCH!>()<!>})
    test(<!TYPE_MISMATCH!>{i: Int64, j: Rune =>}<!>)
}
