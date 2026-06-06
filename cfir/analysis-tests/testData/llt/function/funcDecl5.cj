// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class a {
  func isClosed <!parse_expected_lt_paren!>}<!>
         func <!parse_expected_name!>(<!><!parse_expected_name!>{<!>
  try     (b <!parse_expected_assignment!>a<!><!parse_expected_left_brace, parse_expected_right_delimiter!>
<!>
