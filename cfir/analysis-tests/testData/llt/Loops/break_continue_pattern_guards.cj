// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan 'break' or 'continue' must be used inside a loop

main() {
  for (a in [1] where ([break, continue][0])) { }
  return 0
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
