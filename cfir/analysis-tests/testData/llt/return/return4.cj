// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

func f() {
  {
    <!parse_expected_double_arrow_in_lambda!>func<!> xx(p!:Float64 = 0.1):Float64 {
      1.0
    }
  }
}

main() {
  f()
  return 1
}
