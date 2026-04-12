// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

import define.*

main() {
    var a = '@SAME{1}() <= @SAME{1}()
    var b = @SAME[0]{1}() <= @SAME[0]{1}()
    if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
    {
        return 0
    }
    else
    {
        return 1
    }
}

/* SCAN
error: unterminated single-line string
  ==> timeout_05.cj:13:13:
   | 
13 |     var a = '@SAME{1}() <= @SAME{1}()
   |             ^^^^^^^^^^^^^^^^^^^^^^^^^^ unterminated single-line string
   | 
   # note: consider using multi-line string if you intended to include '\n' in string

1 error generated, 1 error printed.
*/
