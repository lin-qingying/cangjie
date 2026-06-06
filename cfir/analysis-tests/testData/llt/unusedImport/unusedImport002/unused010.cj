// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

import std.collection.{ArrayList as A, <!sema_unused_import!>Map as B<!>}
main() {
  var <!chir_dce_unused_variable!>a<!> : A<Int64>
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
