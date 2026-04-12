// EXEC-PIPE: %compiler %cmp_opt %diag_json %f -o %output 2>&1 | compare %f

import std.deriving.Derive
main() {
}

// ASSERT: scan sema_unused_import



// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
