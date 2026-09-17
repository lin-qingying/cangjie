// F 必须是函数类型；类型预检的主诊断与无法形成类型的后续诊断保留各自 source。
type NonFunction = <!NOT_A_TYPE!>CFunc<!><<!CFUNC_TYPE!>Int64<!>>

main() { 0 }
