// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var list0: Array<Array<Int64>> = [[1, 2, 3, 4], [5, 6, 7]]
    <!TYPE_MISMATCH!>list0[0]++<!>
    <!TYPE_MISMATCH!>list0[0]--<!>
    print("list0[0]: " + list0[0][0].toString())
    if (list0[0][0] == 1) {
        return 0
    }
    return 1
}
