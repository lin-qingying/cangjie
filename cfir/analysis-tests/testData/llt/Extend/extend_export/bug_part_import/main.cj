// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o testa.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o testb.%middle
// EXEC-PIPE-1: %compiler %cmp_opt testb.%middle testa.%middle %f -o %output 2>&1 | compare %f

import a.*
import <!UNUSED_IMPORT!>b.I1<!>

public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> <: A & IChild {}

main() {}
