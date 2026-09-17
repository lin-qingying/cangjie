// CallingConv 参数须为 CDECL 或 STDCALL 引用，不接受同值字符串字面量。
<!ANNOTATION_CALLING_CONV_NOT_SUPPORT!>@CallingConv[FASTCALL]<!>
foreign func unsupportedConvention(): Unit

<!ANNOTATION_INVALID_ARGS_TYPE!>@CallingConv["CDECL"]<!>
foreign func stringConvention(): Unit

main() { 0 }
