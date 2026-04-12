// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./a.cj 
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC-PIPE-0: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

package d

import org1::a.A

main() {}

// ASSERT: scan warning: unused import 'org1::a.A'
