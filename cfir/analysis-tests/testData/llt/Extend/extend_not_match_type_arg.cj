// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// If the type being extended is a generic type, the same generic variant declaration needs to be maintained.

class A<T, R> { }
extend<T, T> A<T, T> { }
extend<T> A<T> { }
extend A { }
extend<T, R, P> A<T, R, P> { }
extend<Int64, R> A<Int64, R> { }

class B { }
extend<T> B<T> { }
extend<Int64> B<Int64> { }
extend<T, Int64> B<T, Int64> { }

/* SCAN
error: expected a generic type name after '<' in generic, found keyword 'Int64'
  ==> extend_not_match_type_arg.cj:16:8:
   | 
16 | extend<Int64, R> A<Int64, R> { }
   |       ~^^^^^ expected a generic type name here
   |       |
   |       after '<' in generic
   | 
help: you could escape keyword as a generic type name using '`':
   | 
16 | extend<`Int64`, R> A<Int64, R> { }
   |        ~~~~~~~ 
   | 

error: expected a generic type name after '<' in generic, found keyword 'Int64'
  ==> extend_not_match_type_arg.cj:20:8:
   | 
20 | extend<Int64> B<Int64> { }
   |       ~^^^^^ expected a generic type name here
   |       |
   |       after '<' in generic
   | 
help: you could escape keyword as a generic type name using '`':
   | 
20 | extend<`Int64`> B<Int64> { }
   |        ~~~~~~~ 
   | 

error: expected a generic type name after ',' in generic, found keyword 'Int64'
  ==> extend_not_match_type_arg.cj:21:11:
   | 
21 | extend<T, Int64> B<T, Int64> { }
   |         ~ ^^^^^ expected a generic type name here
   |         |
   |         after ',' in generic
   | 
help: you could escape keyword as a generic type name using '`':
   | 
21 | extend<T, `Int64`> B<T, Int64> { }
   |           ~~~~~~~ 
   | 

3 errors generated, 3 errors printed.
*/
