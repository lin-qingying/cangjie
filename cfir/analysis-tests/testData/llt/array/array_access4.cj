// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var slicing1 = <!sema_illegal_access_non_static_member!>A<!>rray<Bool>[3]
    <!sema_illegal_access_non_static_member!>A<!>rray<Bool>[3] = 1
    return 0
}
