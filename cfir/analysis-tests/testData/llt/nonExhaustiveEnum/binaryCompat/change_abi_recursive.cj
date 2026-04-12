// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.old.cj %n.new.cj
// EXEC: %compiler %n.old.cj %compile_shared_lib_opt -o liba.%dylib_suffix
// (CJNATIVE) EXEC: %compiler %f -L. -la %cmp_opt -o %output
// (CJNATIVE) EXEC: %compiler %f -L. -la %cmp_opt -o O2.%suffix -O2
// (CJNATIVE) EXEC: %compiler %n.new.cj %compile_shared_lib_opt -o liba.%dylib_suffix
// (CJNATIVE) RUN-EXEC-2: %run %run_opt %output %run_args
// (CJNATIVE) RUN-EXEC-2: %run %run_opt O2.%suffix %run_args

import a.*

main() {
    match (m) {
        case B => 0
        case C(_) => 1
        case _ => 2
    }
}
