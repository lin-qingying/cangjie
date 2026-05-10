// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga.cj pkgb.cj 
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkga.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkgb.cj -o pkgb.%middle
// (CJNATIVE) EXEC: %compiler %cmp_opt %enableO2 %f pkga.%middle pkgb.%middle -o %output 2>&1 | compare %f

package pkg

import pkg.a.<!UNUSED_IMPORT!>ValueType as AValueType<!>
import pkg.b.<!UNUSED_IMPORT!>ValueType as BValueType<!>

main() {}
