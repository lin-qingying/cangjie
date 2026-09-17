// 无参注解报告实际多出的参数，CallingConv 的参数个数错误报告整个注解。
@C[<!ANNOTATION_ERROR_ARG_NUM!>1<!>]
func cFunction(): Unit {}

@FastNative[<!ANNOTATION_ERROR_ARG_NUM!>1<!>]
foreign func nativeFunction(): Unit

@Frozen[<!ANNOTATION_ERROR_ARG_NUM!>1<!>]
func frozenFunction(): Unit {}

<!ANNOTATION_ERROR_ARG_NUM!>@CallingConv[]<!>
foreign func missingConvention(): Unit

<!ANNOTATION_ERROR_ARG_NUM!>@CallingConv[CDECL, STDCALL]<!>
foreign func extraConvention(): Unit

main() { 0 }
