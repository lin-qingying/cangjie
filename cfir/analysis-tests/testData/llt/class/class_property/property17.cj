// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f




main():Int64 {
    return 0
}

open class Boo{
    public open prop func foo(){}
}

class Coo <: Boo{
    public override prop func foo(){}
}

/* SCAN
error: expected a property name after keyword 'prop' in property definition, found keyword 'func'
  ==> property17.cj:17:22:
   | 
17 |     public open prop func foo(){}
   |                 ~~~~ ^^^^ expected a property name here
   |                 |
   |                 after keyword 'prop' in property definition
   | 
help: you could escape keyword as a property name using '`':
   | 
17 |     public open prop `func` foo(){}
   |                      ~~~~~~ 
   | 

error: expected a property name after keyword 'prop' in property definition, found keyword 'func'
  ==> property17.cj:21:26:
   | 
21 |     public override prop func foo(){}
   |                     ~~~~ ^^^^ expected a property name here
   |                     |
   |                     after keyword 'prop' in property definition
   | 
help: you could escape keyword as a property name using '`':
   | 
21 |     public override prop `func` foo(){}
   |                          ~~~~~~ 
   | 

2 errors generated, 2 errors printed.
*/
