// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class C1 {
  public <!parse_unexpected_declaration_in_scope!>interface<!> I1 {}
  public <!parse_unexpected_declaration_in_scope!>class<!> C2 {
    public <!parse_unexpected_declaration_in_scope!>interface<!> I2 {}
 }
}
class C3 <: C1.<!parse_expected_name!>
<!>
