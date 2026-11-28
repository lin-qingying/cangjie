// 单名和嵌套 tuple 全局 let 均保留声明容器；wildcard 不产生 entries。
package test.patterns.symbols

let singleValue = 1
let (number, (flag, text), _) = (2, (true, "ready"), false)
let (_, _) = (3, true)
