// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var (a, b, c) = (0, 1, 2)
    var d = (3, 4, (a, b, c)[<!sema_builtin_index_in_bound!>3<!>])
    var (one, two, three): (Int32, Int32, Int32) = <!sema_mismatched_types!>(5, 6, 7, 8)<!>
    return 0
}
