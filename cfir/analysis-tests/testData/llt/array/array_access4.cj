// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var slicing1 = <!sema_illegal_access_non_static_member!>A<!>rray<Bool>[3]
    <!sema_illegal_access_non_static_member!>A<!>rray<Bool>[3] = 1
    return 0
}
