	.text
	.def	@feat.00;
	.scl	3;
	.type	0;
	.endef
	.globl	@feat.00
.set @feat.00, 0
	.file	"0-ohos.dep"
	.def	_CAN8ohos.dep1AE;
	.scl	2;
	.type	32;
	.endef
	.globl	_CAN8ohos.dep1AE                # -- Begin function _CAN8ohos.dep1AE
	.p2align	4, 0x90
.Ltmp0:                                 # @_CAN8ohos.dep1AE
	.long	.Lmethod_desc._CAN8ohos.dep1AE-.Ltmp0
_CAN8ohos.dep1AE:
.Lfunc_begin0:
.seh_proc _CAN8ohos.dep1AE
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$80, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow0
.Lstack.check.end0:
	.seh_stackalloc 80
	leaq	80(%rsp), %rbp
	.seh_setframe %rbp, 80
	.seh_endprologue
	movq	%rcx, -32(%rbp)                 # 8-byte Spill
	movq	%rcx, -16(%rbp)                 # 8-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp1
.Ltmp2:
	leaq	"RawArray<std.core:Object>.ti"(%rip), %rcx
	movl	$1, %edx
	movl	$24, %r8d
.LNewArrayFastPath0:
	movq	(%r15), %r9
	movq	(%r9), %r9
	movq	(%r9), %rax
	movq	8(%r9), %r10
	leaq	(%rax,%r8), %r11
	cmpq	%r10, %r11
	jg	.LNewArraySlowPath0
	movq	%rcx, (%rax)
	movq	%rdx, 8(%rax)
	movq	%r11, (%r9)
	jmp	.LNewArrayFin0
.LNewArraySlowPath0:
	callq	CJ_MCC_NewObjArray
.LNewArrayFin0:
.Ltmp3:
	movq	-32(%rbp), %rcx                 # 8-byte Reload
	movq	%rax, %r8
	movq	%r8, -8(%rbp)
	leaq	"std.core:Array<std.core:Object>.ti"(%rip), %rdx
	movq	%rsp, %rax
	movq	%rdx, 40(%rax)
	movq	$1, 32(%rax)
	xorl	%eax, %eax
	movl	%eax, %r9d
	movq	%r9, %rdx
	callq	"_CNat5ArrayIG_E6<init>HA0_G_ll"
.Ltmp4:
	movq	-8(%rbp), %rax
	movq	%rax, -24(%rbp)                 # 8-byte Spill
	leaq	_CAN8ohos.dep1AE_0(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$8, %r8
	callq	CJ_MCC_WriteRefField
	movq	-16(%rbp), %rax                 # 8-byte Reload
.L_epilogue0:
	movq	%rbp, %rsp
	subq	$80, %rsp
	addq	$80, %rsp
	popq	%rbp
	retq
.Lstack.overflow0:
	addq	$80, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp5:
	subq	$80, %rsp
	jmp	.Lstack.check.end0
.Ltmp1:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp6:
	jmp	.Ltmp2
.Lfunc_end0:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table0:
.Lexception0:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	.L__cj_personality_v0$;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function __cj_personality_v0$
.L__cj_personality_v0$:                 # @"__cj_personality_v0$"
# %bb.0:                                # %entry
	xorl	%eax, %eax
	retq
                                        # -- End function
	.def	_CAN8ohos.dep1BE;
	.scl	2;
	.type	32;
	.endef
	.globl	_CAN8ohos.dep1BE                # -- Begin function _CAN8ohos.dep1BE
	.p2align	4, 0x90
.Ltmp7:                                 # @_CAN8ohos.dep1BE
	.long	.Lmethod_desc._CAN8ohos.dep1BE-.Ltmp7
_CAN8ohos.dep1BE:
.Lfunc_begin1:
.seh_proc _CAN8ohos.dep1BE
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$80, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow1
.Lstack.check.end1:
	.seh_stackalloc 80
	leaq	80(%rsp), %rbp
	.seh_setframe %rbp, 80
	.seh_endprologue
	movq	%rcx, -32(%rbp)                 # 8-byte Spill
	movq	%rcx, -16(%rbp)                 # 8-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp8
.Ltmp9:
	leaq	"RawArray<std.core:Object>.ti"(%rip), %rcx
	movl	$2, %edx
	movl	$32, %r8d
.LNewArrayFastPath1:
	movq	(%r15), %r9
	movq	(%r9), %r9
	movq	(%r9), %rax
	movq	8(%r9), %r10
	leaq	(%rax,%r8), %r11
	cmpq	%r10, %r11
	jg	.LNewArraySlowPath1
	movq	%rcx, (%rax)
	movq	%rdx, 8(%rax)
	movq	%r11, (%r9)
	jmp	.LNewArrayFin1
.LNewArraySlowPath1:
	callq	CJ_MCC_NewObjArray
.LNewArrayFin1:
.Ltmp10:
	movq	-32(%rbp), %rcx                 # 8-byte Reload
	movq	%rax, %r8
	movq	%r8, -8(%rbp)
	leaq	"std.core:Array<std.core:Object>.ti"(%rip), %rdx
	movq	%rsp, %rax
	movq	%rdx, 40(%rax)
	movq	$2, 32(%rax)
	xorl	%eax, %eax
	movl	%eax, %r9d
	movq	%r9, %rdx
	callq	"_CNat5ArrayIG_E6<init>HA0_G_ll"
.Ltmp11:
	movq	-8(%rbp), %rax
	movq	%rax, -24(%rbp)                 # 8-byte Spill
	leaq	_CAN8ohos.dep1BE_0(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$8, %r8
	callq	CJ_MCC_WriteRefField
	leaq	_CAN8ohos.dep1BE_1(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$16, %r8
	callq	CJ_MCC_WriteRefField
	movq	-16(%rbp), %rax                 # 8-byte Reload
.L_epilogue1:
	movq	%rbp, %rsp
	subq	$80, %rsp
	addq	$80, %rsp
	popq	%rbp
	retq
.Lstack.overflow1:
	addq	$80, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp12:
	subq	$80, %rsp
	jmp	.Lstack.check.end1
.Ltmp8:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp13:
	jmp	.Ltmp9
.Lfunc_end1:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table2:
.Lexception1:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	_CAN8ohos.dep1CE;
	.scl	2;
	.type	32;
	.endef
	.globl	_CAN8ohos.dep1CE                # -- Begin function _CAN8ohos.dep1CE
	.p2align	4, 0x90
.Ltmp14:                                # @_CAN8ohos.dep1CE
	.long	.Lmethod_desc._CAN8ohos.dep1CE-.Ltmp14
_CAN8ohos.dep1CE:
.Lfunc_begin2:
.seh_proc _CAN8ohos.dep1CE
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$80, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow2
.Lstack.check.end2:
	.seh_stackalloc 80
	leaq	80(%rsp), %rbp
	.seh_setframe %rbp, 80
	.seh_endprologue
	movq	%rcx, -32(%rbp)                 # 8-byte Spill
	movq	%rcx, -16(%rbp)                 # 8-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp15
.Ltmp16:
	leaq	"RawArray<std.core:Object>.ti"(%rip), %rcx
	movl	$1, %edx
	movl	$24, %r8d
.LNewArrayFastPath2:
	movq	(%r15), %r9
	movq	(%r9), %r9
	movq	(%r9), %rax
	movq	8(%r9), %r10
	leaq	(%rax,%r8), %r11
	cmpq	%r10, %r11
	jg	.LNewArraySlowPath2
	movq	%rcx, (%rax)
	movq	%rdx, 8(%rax)
	movq	%r11, (%r9)
	jmp	.LNewArrayFin2
.LNewArraySlowPath2:
	callq	CJ_MCC_NewObjArray
.LNewArrayFin2:
.Ltmp17:
	movq	-32(%rbp), %rcx                 # 8-byte Reload
	movq	%rax, %r8
	movq	%r8, -8(%rbp)
	leaq	"std.core:Array<std.core:Object>.ti"(%rip), %rdx
	movq	%rsp, %rax
	movq	%rdx, 40(%rax)
	movq	$1, 32(%rax)
	xorl	%eax, %eax
	movl	%eax, %r9d
	movq	%r9, %rdx
	callq	"_CNat5ArrayIG_E6<init>HA0_G_ll"
.Ltmp18:
	movq	-8(%rbp), %rax
	movq	%rax, -24(%rbp)                 # 8-byte Spill
	leaq	_CAN8ohos.dep1CE_0(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$8, %r8
	callq	CJ_MCC_WriteRefField
	movq	-16(%rbp), %rax                 # 8-byte Reload
.L_epilogue2:
	movq	%rbp, %rsp
	subq	$80, %rsp
	addq	$80, %rsp
	popq	%rbp
	retq
.Lstack.overflow2:
	addq	$80, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp19:
	subq	$80, %rsp
	jmp	.Lstack.check.end2
.Ltmp15:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp20:
	jmp	.Ltmp16
.Lfunc_end2:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table3:
.Lexception2:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	_CAN8ohos.dep1DE;
	.scl	2;
	.type	32;
	.endef
	.globl	_CAN8ohos.dep1DE                # -- Begin function _CAN8ohos.dep1DE
	.p2align	4, 0x90
.Ltmp21:                                # @_CAN8ohos.dep1DE
	.long	.Lmethod_desc._CAN8ohos.dep1DE-.Ltmp21
_CAN8ohos.dep1DE:
.Lfunc_begin3:
.seh_proc _CAN8ohos.dep1DE
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$80, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow3
.Lstack.check.end3:
	.seh_stackalloc 80
	leaq	80(%rsp), %rbp
	.seh_setframe %rbp, 80
	.seh_endprologue
	movq	%rcx, -32(%rbp)                 # 8-byte Spill
	movq	%rcx, -16(%rbp)                 # 8-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp22
.Ltmp23:
	leaq	"RawArray<std.core:Object>.ti"(%rip), %rcx
	movl	$2, %edx
	movl	$32, %r8d
.LNewArrayFastPath3:
	movq	(%r15), %r9
	movq	(%r9), %r9
	movq	(%r9), %rax
	movq	8(%r9), %r10
	leaq	(%rax,%r8), %r11
	cmpq	%r10, %r11
	jg	.LNewArraySlowPath3
	movq	%rcx, (%rax)
	movq	%rdx, 8(%rax)
	movq	%r11, (%r9)
	jmp	.LNewArrayFin3
.LNewArraySlowPath3:
	callq	CJ_MCC_NewObjArray
.LNewArrayFin3:
.Ltmp24:
	movq	-32(%rbp), %rcx                 # 8-byte Reload
	movq	%rax, %r8
	movq	%r8, -8(%rbp)
	leaq	"std.core:Array<std.core:Object>.ti"(%rip), %rdx
	movq	%rsp, %rax
	movq	%rdx, 40(%rax)
	movq	$2, 32(%rax)
	xorl	%eax, %eax
	movl	%eax, %r9d
	movq	%r9, %rdx
	callq	"_CNat5ArrayIG_E6<init>HA0_G_ll"
.Ltmp25:
	movq	-8(%rbp), %rax
	movq	%rax, -24(%rbp)                 # 8-byte Spill
	leaq	_CAN8ohos.dep1DE_0(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$8, %r8
	callq	CJ_MCC_WriteRefField
	leaq	_CAN8ohos.dep1DE_1(%rip), %rcx
	callq	CJ_MCC_ReadStaticRef
	movq	-24(%rbp), %rdx                 # 8-byte Reload
	movq	%rax, %rcx
	movq	%rdx, %r8
	addq	$8, %r8
	addq	$16, %r8
	callq	CJ_MCC_WriteRefField
	movq	-16(%rbp), %rax                 # 8-byte Reload
.L_epilogue3:
	movq	%rbp, %rsp
	subq	$80, %rsp
	addq	$80, %rsp
	popq	%rbp
	retq
.Lstack.overflow3:
	addq	$80, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp26:
	subq	$80, %rsp
	jmp	.Lstack.check.end3
.Ltmp22:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp27:
	jmp	.Ltmp23
.Lfunc_end3:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table4:
.Lexception3:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	_CGF8ohos.dep1AE_0;
	.scl	3;
	.type	32;
	.endef
	.section	.cjinit_function,"xr"
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1AE_0
.Ltmp28:                                # @_CGF8ohos.dep1AE_0
	.long	.Lmethod_desc._CGF8ohos.dep1AE_0-.Ltmp28
_CGF8ohos.dep1AE_0:
.Lfunc_begin4:
.seh_proc _CGF8ohos.dep1AE_0
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movq	".refptr.ohos.labels:APILevel.ti"(%rip), %rcx
	movl	12(%rcx), %edx
	addl	$15, %edx
	andl	$-8, %edx
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp29
.Ltmp30:
	callq	CJ_MCC_NewObjectFast
.Ltmp31:
	movq	%rax, %rdx
	movq	%rdx, -8(%rbp)                  # 8-byte Spill
	movq	%rdx, %r8
	addq	$8, %r8
	movq	".L$const_cjstring.efd9tk-novH"(%rip), %rcx
	callq	CJ_MCC_WriteRefField
	movq	-8(%rbp), %rcx                  # 8-byte Reload
	movq	".L$const_cjstring.efd9tk-novH"+8(%rip), %rax
	movq	%rax, 16(%rcx)
	leaq	_CAN8ohos.dep1AE_0(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue4:
	movq	%rbp, %rsp
	subq	$48, %rsp
	addq	$48, %rsp
	popq	%rbp
	retq
.Ltmp29:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp32:
	jmp	.Ltmp30
.Lfunc_end4:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table5:
.Lexception4:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGF8ohos.dep1BE_0;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1BE_0
.Ltmp33:                                # @_CGF8ohos.dep1BE_0
	.long	.Lmethod_desc._CGF8ohos.dep1BE_0-.Ltmp33
_CGF8ohos.dep1BE_0:
.Lfunc_begin5:
.seh_proc _CGF8ohos.dep1BE_0
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movq	".refptr.ohos.labels:APILevel.ti"(%rip), %rcx
	movl	12(%rcx), %edx
	addl	$15, %edx
	andl	$-8, %edx
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp34
.Ltmp35:
	callq	CJ_MCC_NewObjectFast
.Ltmp36:
	movq	%rax, %rdx
	movq	%rdx, -8(%rbp)                  # 8-byte Spill
	movq	%rdx, %r8
	addq	$8, %r8
	movq	".L$const_cjstring.efd9tk-novH"(%rip), %rcx
	callq	CJ_MCC_WriteRefField
	movq	-8(%rbp), %rcx                  # 8-byte Reload
	movq	".L$const_cjstring.efd9tk-novH"+8(%rip), %rax
	movq	%rax, 16(%rcx)
	leaq	_CAN8ohos.dep1BE_0(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue5:
	movq	%rbp, %rsp
	subq	$48, %rsp
	addq	$48, %rsp
	popq	%rbp
	retq
.Ltmp34:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp37:
	jmp	.Ltmp35
.Lfunc_end5:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table6:
.Lexception5:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGF8ohos.dep1BE_1;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1BE_1
.Ltmp38:                                # @_CGF8ohos.dep1BE_1
	.long	.Lmethod_desc._CGF8ohos.dep1BE_1-.Ltmp38
_CGF8ohos.dep1BE_1:
.Lfunc_begin6:
.seh_proc _CGF8ohos.dep1BE_1
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$32, %rsp
	.seh_stackalloc 32
	leaq	32(%rsp), %rbp
	.seh_setframe %rbp, 32
	.seh_endprologue
	movq	".refptr.anno:CustomAnno.ti"(%rip), %rcx
	movl	12(%rcx), %edx
	addl	$15, %edx
	andl	$-8, %edx
	callq	CJ_MCC_NewObjectFast
.Ltmp39:
	movq	%rax, %rcx
	leaq	_CAN8ohos.dep1BE_1(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue6:
	movq	%rbp, %rsp
	subq	$32, %rsp
	addq	$32, %rsp
	popq	%rbp
	retq
.Lfunc_end6:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table7:
.Lexception6:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGF8ohos.dep1CE_0;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1CE_0
.Ltmp40:                                # @_CGF8ohos.dep1CE_0
	.long	.Lmethod_desc._CGF8ohos.dep1CE_0-.Ltmp40
_CGF8ohos.dep1CE_0:
.Lfunc_begin7:
.seh_proc _CGF8ohos.dep1CE_0
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$32, %rsp
	.seh_stackalloc 32
	leaq	32(%rsp), %rbp
	.seh_setframe %rbp, 32
	.seh_endprologue
	movq	".refptr.ohos.labels:Hide.ti"(%rip), %rcx
	movl	$8, %edx
	callq	CJ_MCC_NewObjectFast
.Ltmp41:
	movq	%rax, %rcx
	leaq	_CAN8ohos.dep1CE_0(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue7:
	movq	%rbp, %rsp
	subq	$32, %rsp
	addq	$32, %rsp
	popq	%rbp
	retq
.Lfunc_end7:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table8:
.Lexception7:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGF8ohos.dep1DE_0;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1DE_0
.Ltmp42:                                # @_CGF8ohos.dep1DE_0
	.long	.Lmethod_desc._CGF8ohos.dep1DE_0-.Ltmp42
_CGF8ohos.dep1DE_0:
.Lfunc_begin8:
.seh_proc _CGF8ohos.dep1DE_0
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$32, %rsp
	.seh_stackalloc 32
	leaq	32(%rsp), %rbp
	.seh_setframe %rbp, 32
	.seh_endprologue
	movq	".refptr.ohos.labels:Hide.ti"(%rip), %rcx
	movl	$8, %edx
	callq	CJ_MCC_NewObjectFast
.Ltmp43:
	movq	%rax, %rcx
	leaq	_CAN8ohos.dep1DE_0(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue8:
	movq	%rbp, %rsp
	subq	$32, %rsp
	addq	$32, %rsp
	popq	%rbp
	retq
.Lfunc_end8:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table9:
.Lexception8:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGF8ohos.dep1DE_1;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function _CGF8ohos.dep1DE_1
.Ltmp44:                                # @_CGF8ohos.dep1DE_1
	.long	.Lmethod_desc._CGF8ohos.dep1DE_1-.Ltmp44
_CGF8ohos.dep1DE_1:
.Lfunc_begin9:
.seh_proc _CGF8ohos.dep1DE_1
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb1
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$32, %rsp
	.seh_stackalloc 32
	leaq	32(%rsp), %rbp
	.seh_setframe %rbp, 32
	.seh_endprologue
	movq	".refptr.anno:CustomAnno.ti"(%rip), %rcx
	movl	12(%rcx), %edx
	addl	$15, %edx
	andl	$-8, %edx
	callq	CJ_MCC_NewObjectFast
.Ltmp45:
	movq	%rax, %rcx
	leaq	_CAN8ohos.dep1DE_1(%rip), %rdx
	callq	CJ_MCC_WriteStaticRef
	nop
.L_epilogue9:
	movq	%rbp, %rsp
	subq	$32, %rsp
	addq	$32, %rsp
	popq	%rbp
	retq
.Lfunc_end9:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table10:
.Lexception9:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGP8ohos.depiiHv;
	.scl	2;
	.type	32;
	.endef
	.globl	_CGP8ohos.depiiHv               # -- Begin function _CGP8ohos.depiiHv
	.p2align	4, 0x90
.Ltmp46:                                # @_CGP8ohos.depiiHv
	.long	.Lmethod_desc._CGP8ohos.depiiHv-.Ltmp46
_CGP8ohos.depiiHv:
.Lfunc_begin10:
.seh_proc _CGP8ohos.depiiHv
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow4
.Lstack.check.end4:
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movb	($has_applied_pkg_init_func)(%rip), %al
	movb	%al, -1(%rbp)                   # 1-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp47
.Ltmp48:
	movb	-1(%rbp), %al                   # 1-byte Reload
	testb	$1, %al
	jne	.LBB11_1
	jmp	.LBB11_2
.LBB11_1:                               # %bb1
.L_epilogue10:
	movq	%rbp, %rsp
	subq	$48, %rsp
	addq	$48, %rsp
	popq	%rbp
	retq
.LBB11_2:                               # %bb2
	movb	$1, ($has_applied_pkg_init_func)(%rip)
	callq	_CGP4annoiiHv
.Ltmp49:
	callq	_CGP11ohos.labelsiiHv
.Ltmp50:
	callq	_CGPatiiHv
.Ltmp51:
	xorl	%eax, %eax
	movl	%eax, %ecx
	leaq	__CJMetadataStart(%rip), %rcx
	callq	CJ_MRT_PreInitializePackage
	callq	_CGF8ohos.dep1AE_0
.Ltmp52:
	callq	_CGF8ohos.dep1BE_0
.Ltmp53:
	callq	_CGF8ohos.dep1BE_1
.Ltmp54:
	callq	_CGF8ohos.dep1CE_0
.Ltmp55:
	callq	_CGF8ohos.dep1DE_0
.Ltmp56:
	callq	_CGF8ohos.dep1DE_1
.Ltmp57:
	nop
	addq	$48, %rsp
	popq	%rbp
	retq
.Lstack.overflow4:
	addq	$48, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp58:
	subq	$48, %rsp
	jmp	.Lstack.check.end4
.Ltmp47:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp59:
	jmp	.Ltmp48
.Lfunc_end10:
	.seh_handlerdata
	.section	.cjinit_function,"xr"
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table11:
.Lexception10:
	.long	1431655765                      # No callsite function EH table header
	.section	.cjinit_function,"xr"
                                        # -- End function
	.def	_CGP8ohos.depilHv;
	.scl	2;
	.type	32;
	.endef
	.text
	.globl	_CGP8ohos.depilHv               # -- Begin function _CGP8ohos.depilHv
	.p2align	4, 0x90
.Ltmp60:                                # @_CGP8ohos.depilHv
	.long	.Lmethod_desc._CGP8ohos.depilHv-.Ltmp60
_CGP8ohos.depilHv:
.Lfunc_begin11:
.seh_proc _CGP8ohos.depilHv
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow5
.Lstack.check.end5:
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movb	has_invoked_pkg_init_literal(%rip), %al
	movb	%al, -1(%rbp)                   # 1-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp61
.Ltmp62:
	movb	-1(%rbp), %al                   # 1-byte Reload
	testb	$1, %al
	jne	.LBB12_1
	jmp	.LBB12_2
.LBB12_1:                               # %bb1
.L_epilogue11:
	movq	%rbp, %rsp
	subq	$48, %rsp
	addq	$48, %rsp
	popq	%rbp
	retq
.LBB12_2:                               # %bb2
	movb	$1, has_invoked_pkg_init_literal(%rip)
	callq	_CGP4annoilHv
.Ltmp63:
	callq	_CGP11ohos.labelsilHv
.Ltmp64:
	callq	_CGPatilHv
.Ltmp65:
	addq	$48, %rsp
	popq	%rbp
	retq
.Lstack.overflow5:
	addq	$48, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp66:
	subq	$48, %rsp
	jmp	.Lstack.check.end5
.Ltmp61:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp67:
	jmp	.Ltmp62
.Lfunc_end11:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table12:
.Lexception11:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	"_CN8ohos.dep1A6<init>Hv";
	.scl	2;
	.type	32;
	.endef
	.globl	"_CN8ohos.dep1A6<init>Hv"       # -- Begin function _CN8ohos.dep1A6<init>Hv
	.p2align	4, 0x90
"_CN8ohos.dep1A6<init>Hv":              # @"_CN8ohos.dep1A6<init>Hv"
.Lfunc_begin12:
.seh_proc "_CN8ohos.dep1A6<init>Hv"
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	retq
.Lfunc_end12:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table13:
.Lexception12:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	"_CN8ohos.dep1B6<init>Hv";
	.scl	2;
	.type	32;
	.endef
	.globl	"_CN8ohos.dep1B6<init>Hv"       # -- Begin function _CN8ohos.dep1B6<init>Hv
	.p2align	4, 0x90
"_CN8ohos.dep1B6<init>Hv":              # @"_CN8ohos.dep1B6<init>Hv"
.Lfunc_begin13:
.seh_proc "_CN8ohos.dep1B6<init>Hv"
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	retq
.Lfunc_end13:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table14:
.Lexception13:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	"_CN8ohos.dep1C6<init>Hv";
	.scl	2;
	.type	32;
	.endef
	.globl	"_CN8ohos.dep1C6<init>Hv"       # -- Begin function _CN8ohos.dep1C6<init>Hv
	.p2align	4, 0x90
"_CN8ohos.dep1C6<init>Hv":              # @"_CN8ohos.dep1C6<init>Hv"
.Lfunc_begin14:
.seh_proc "_CN8ohos.dep1C6<init>Hv"
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	retq
.Lfunc_end14:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table15:
.Lexception14:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	"_CN8ohos.dep1D6<init>Hv";
	.scl	2;
	.type	32;
	.endef
	.globl	"_CN8ohos.dep1D6<init>Hv"       # -- Begin function _CN8ohos.dep1D6<init>Hv
	.p2align	4, 0x90
"_CN8ohos.dep1D6<init>Hv":              # @"_CN8ohos.dep1D6<init>Hv"
.Lfunc_begin15:
.seh_proc "_CN8ohos.dep1D6<init>Hv"
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %bb0
	retq
.Lfunc_end15:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table16:
.Lexception15:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	_CGP8ohos.depirHv;
	.scl	2;
	.type	32;
	.endef
	.globl	_CGP8ohos.depirHv               # -- Begin function _CGP8ohos.depirHv
	.p2align	4, 0x90
.Ltmp68:                                # @_CGP8ohos.depirHv
	.long	.Lmethod_desc._CGP8ohos.depirHv-.Ltmp68
_CGP8ohos.depirHv:
.Lfunc_begin16:
.seh_proc _CGP8ohos.depirHv
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %entry
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow6
.Lstack.check.end6:
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movq	%rcx, -8(%rbp)                  # 8-byte Spill
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp69
.Ltmp70:
	callq	_CGP8ohos.depifHv
.Ltmp71:
	callq	_CGP8ohos.depilHv
.Ltmp72:
	callq	_CGP8ohos.depiiHv
.Ltmp73:
	movq	-8(%rbp), %rax                  # 8-byte Reload
	movq	(%rax), %rcx
	movq	8(%rax), %rdx
	movq	wrapper.F0uPuE.CJStubGV(%rip), %r11
	pushq	%r11
	pushq	$32
	callq	CJ_MCC_C2NStub
.Ltmp74:
.L_epilogue12:
	movq	%rbp, %rsp
	subq	$48, %rsp
	addq	$48, %rsp
	popq	%rbp
	retq
.Lstack.overflow6:
	addq	$48, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp75:
	subq	$48, %rsp
	jmp	.Lstack.check.end6
.Ltmp69:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp76:
	jmp	.Ltmp70
.Lfunc_end16:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table17:
.Lexception16:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	_CGP8ohos.depifHv;
	.scl	2;
	.type	32;
	.endef
	.globl	_CGP8ohos.depifHv               # -- Begin function _CGP8ohos.depifHv
	.p2align	4, 0x90
.Ltmp77:                                # @_CGP8ohos.depifHv
	.long	.Lmethod_desc._CGP8ohos.depifHv-.Ltmp77
_CGP8ohos.depifHv:
.Lfunc_begin17:
.seh_proc _CGP8ohos.depifHv
	.seh_handler .L__cj_personality_v0$, @unwind, @except
# %bb.0:                                # %entry
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$32, %rsp
	movq	40(%r15), %rax
	cmpq	%rax, %rsp
	jbe	.Lstack.overflow7
.Lstack.check.end7:
	.seh_stackalloc 32
	leaq	32(%rsp), %rbp
	.seh_setframe %rbp, 32
	.seh_endprologue
	movq	48(%r15), %rax
	cmpq	$0, %rax
	jne	.Ltmp78
.Ltmp79:
	callq	_CGP4annoifHv
.Ltmp80:
	callq	_CGP11ohos.labelsifHv
.Ltmp81:
	callq	_CGPatifHv
.Ltmp82:
	movb	$0, has_invoked_pkg_init_literal(%rip)
	movb	$0, ($has_applied_pkg_init_func)(%rip)
.L_epilogue13:
	movq	%rbp, %rsp
	subq	$32, %rsp
	addq	$32, %rsp
	popq	%rbp
	retq
.Lstack.overflow7:
	addq	$32, %rsp
	movabsq	$16, %rax
	callq	CJ_MCC_StackGrowStub
.Ltmp83:
	subq	$32, %rsp
	jmp	.Lstack.check.end7
.Ltmp78:
	movq	CJ_MCC_HandleSafepoint.CJStubGV(%rip), %rax
	callq	*%rax
.Ltmp84:
	jmp	.Ltmp79
.Lfunc_end17:
	.seh_handlerdata
	.text
	.seh_endproc
	.section	.xdata,"dw"
	.p2align	2
CJ_except_table18:
.Lexception17:
	.long	1431655765                      # No callsite function EH table header
	.text
                                        # -- End function
	.def	.Lwrapper.F0uPuE;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function wrapper.F0uPuE
.Lwrapper.F0uPuE:                       # @wrapper.F0uPuE
.seh_proc .Lwrapper.F0uPuE
# %bb.0:                                # %entry
	pushq	%rbp
	.seh_pushreg %rbp
	subq	$48, %rsp
	.seh_stackalloc 48
	leaq	48(%rsp), %rbp
	.seh_setframe %rbp, 48
	.seh_endprologue
	movq	%rdx, -8(%rbp)                  # 8-byte Spill
	movq	%rcx, %rax
	movq	-8(%rbp), %rcx                  # 8-byte Reload
	callq	*%rax
	nop
	addq	$48, %rsp
	popq	%rbp
	retq
	.seh_endproc
                                        # -- End function
	.def	CJ_MCC_NewObjectFast;
	.scl	3;
	.type	32;
	.endef
	.p2align	4, 0x90                         # -- Begin function CJ_MCC_NewObjectFast
CJ_MCC_NewObjectFast:                   # @CJ_MCC_NewObjectFast
# %bb.0:                                # %entry
	#ADJCALLSTACKDOWN
	movq	(%r15), %r9
	movq	(%r9), %r9
	movq	(%r9), %rax
	movq	8(%r9), %r10
	leaq	(%rax,%rdx), %r8
	cmpq	%r10, %r8
	jg	CJ_MCC_NewObject
	movq	%rcx, (%rax)
	movq	%r8, (%r9)
.Ltmp85:
	#ADJCALLSTACKUP
	retq
                                        # -- End function
	.bss
	.p2align	3                               # @_CAN8ohos.dep1AE_0
.LRef._CAN8ohos.dep1AE_0:
_CAN8ohos.dep1AE_0:
	.quad	0

	.p2align	3                               # @_CAN8ohos.dep1BE_0
.LRef._CAN8ohos.dep1BE_0:
_CAN8ohos.dep1BE_0:
	.quad	0

	.p2align	3                               # @_CAN8ohos.dep1BE_1
.LRef._CAN8ohos.dep1BE_1:
_CAN8ohos.dep1BE_1:
	.quad	0

	.p2align	3                               # @_CAN8ohos.dep1CE_0
.LRef._CAN8ohos.dep1CE_0:
_CAN8ohos.dep1CE_0:
	.quad	0

	.p2align	3                               # @_CAN8ohos.dep1DE_0
.LRef._CAN8ohos.dep1DE_0:
_CAN8ohos.dep1DE_0:
	.quad	0

	.p2align	3                               # @_CAN8ohos.dep1DE_1
.LRef._CAN8ohos.dep1DE_1:
_CAN8ohos.dep1DE_1:
	.quad	0

	.p2align	3                               # @"$has_applied_pkg_init_func"
.LRef.$has_applied_pkg_init_func:
$has_applied_pkg_init_func:
	.byte	0                               # 0x0

	.p2align	3                               # @has_invoked_pkg_init_literal
.LRef.has_invoked_pkg_init_literal:
has_invoked_pkg_init_literal:
	.byte	0                               # 0x0

	.section	.rdata,"dr"
	.p2align	3                               # @"$const_cjstring.efd9tk-novH"
".L$const_cjstring.efd9tk-novH":
	.quad	.L$const_cjstring_data.6zKVsbikJlq
	.long	41                              # 0x29
	.long	2                               # 0x2

	.data
.LRef..Lcj.sdk.version:                 # @cj.sdk.version
.Lcj.sdk.version:
	.asciz	"1.0.5"

	.section	.rdata,"dr"
	.p2align	4                               # @"$const_cjstring_data.6zKVsbikJlq"
.L$const_cjstring_data.6zKVsbikJlq:
	.quad	"RawArray<UInt8>.ti"
	.quad	43                              # 0x2b
	.ascii	"The value of the step should not be zero.21"
	.zero	5

	.data
	.p2align	3                               # @wrapper.F0uPuE.CJStubGV
wrapper.F0uPuE.CJStubGV:
	.quad	.Lwrapper.F0uPuE

	.p2align	3                               # @CJ_MCC_HandleSafepoint.CJStubGV
CJ_MCC_HandleSafepoint.CJStubGV:
	.quad	CJ_MCC_HandleSafepoint

	.section	.rdata$.refptr.anno:CustomAnno.ti,"dr",discard,".refptr.anno:CustomAnno.ti"
	.p2align	3
	.globl	".refptr.anno:CustomAnno.ti"
".refptr.anno:CustomAnno.ti":
	.quad	"anno:CustomAnno.ti"
	.section	.rdata$.refptr.ohos.labels:APILevel.ti,"dr",discard,".refptr.ohos.labels:APILevel.ti"
	.p2align	3
	.globl	".refptr.ohos.labels:APILevel.ti"
".refptr.ohos.labels:APILevel.ti":
	.quad	"ohos.labels:APILevel.ti"
	.section	.rdata$.refptr.ohos.labels:Hide.ti,"dr",discard,".refptr.ohos.labels:Hide.ti"
	.p2align	3
	.globl	".refptr.ohos.labels:Hide.ti"
".refptr.ohos.labels:Hide.ti":
	.quad	"ohos.labels:Hide.ti"
	.section	.cjsdkv,"dw"
	.quad	.LRef..Lcj.sdk.version
	.section	.cjsm,"dw"
.Lstack_map._CAN8ohos.dep1AE:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:2
	.byte	0
	.byte	32
	.byte	34
	.byte	18
	.byte	114
	.byte	4
	.long	.Ltmp4-_CAN8ohos.dep1AE
	#[RegIdx: -1, SlotIdx: 0, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 1]
	.byte	4
	.byte	4
	.long	.Ltmp5-_CAN8ohos.dep1AE
	#[RegIdx: -1, SlotIdx: -1, LNIdx: 0, DerivedStartIdx: -1, SPRegIdx: 0, SPSlotIdx: -1]
	.byte	144
	.byte	0
	#RegNums: 1
		#Idx[0]: (0x106=262), rdx, rcx, r8
	#SlotsNums: 2
		#Idx[0]: BaseOffset: -8, SlotBits: 0x1[ -8 ]
		#Idx[1]: BaseOffset: -16, SlotBits: 0x1[ -16 ]
	#LineNumbersNums: 1
		#Idx[0]: 7
	#DerivedInfoNums: 0
	.byte	145
	.byte	6
	.byte	5
	.byte	3
	.byte	63
	.byte	252
	.byte	152
	.byte	3
.Lstack_map._CAN8ohos.dep1BE:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:2
	.byte	0
	.byte	32
	.byte	34
	.byte	18
	.byte	114
	.byte	4
	.long	.Ltmp11-_CAN8ohos.dep1BE
	#[RegIdx: -1, SlotIdx: 0, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 1]
	.byte	4
	.byte	4
	.long	.Ltmp12-_CAN8ohos.dep1BE
	#[RegIdx: -1, SlotIdx: -1, LNIdx: 0, DerivedStartIdx: -1, SPRegIdx: 0, SPSlotIdx: -1]
	.byte	144
	.byte	0
	#RegNums: 1
		#Idx[0]: (0x106=262), rdx, rcx, r8
	#SlotsNums: 2
		#Idx[0]: BaseOffset: -8, SlotBits: 0x1[ -8 ]
		#Idx[1]: BaseOffset: -16, SlotBits: 0x1[ -16 ]
	#LineNumbersNums: 1
		#Idx[0]: 11
	#DerivedInfoNums: 0
	.byte	145
	.byte	6
	.byte	5
	.byte	3
	.byte	63
	.byte	252
	.byte	160
	.byte	5
.Lstack_map._CAN8ohos.dep1CE:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:2
	.byte	0
	.byte	32
	.byte	34
	.byte	18
	.byte	114
	.byte	4
	.long	.Ltmp18-_CAN8ohos.dep1CE
	#[RegIdx: -1, SlotIdx: 0, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 1]
	.byte	4
	.byte	4
	.long	.Ltmp19-_CAN8ohos.dep1CE
	#[RegIdx: -1, SlotIdx: -1, LNIdx: 0, DerivedStartIdx: -1, SPRegIdx: 0, SPSlotIdx: -1]
	.byte	144
	.byte	0
	#RegNums: 1
		#Idx[0]: (0x106=262), rdx, rcx, r8
	#SlotsNums: 2
		#Idx[0]: BaseOffset: -8, SlotBits: 0x1[ -8 ]
		#Idx[1]: BaseOffset: -16, SlotBits: 0x1[ -16 ]
	#LineNumbersNums: 1
		#Idx[0]: 15
	#DerivedInfoNums: 0
	.byte	145
	.byte	6
	.byte	5
	.byte	3
	.byte	63
	.byte	252
	.byte	160
	.byte	7
.Lstack_map._CAN8ohos.dep1DE:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:2
	.byte	0
	.byte	32
	.byte	34
	.byte	18
	.byte	114
	.byte	4
	.long	.Ltmp25-_CAN8ohos.dep1DE
	#[RegIdx: -1, SlotIdx: 0, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 1]
	.byte	4
	.byte	4
	.long	.Ltmp26-_CAN8ohos.dep1DE
	#[RegIdx: -1, SlotIdx: -1, LNIdx: 0, DerivedStartIdx: -1, SPRegIdx: 0, SPSlotIdx: -1]
	.byte	144
	.byte	0
	#RegNums: 1
		#Idx[0]: (0x106=262), rdx, rcx, r8
	#SlotsNums: 2
		#Idx[0]: BaseOffset: -8, SlotBits: 0x1[ -8 ]
		#Idx[1]: BaseOffset: -16, SlotBits: 0x1[ -16 ]
	#LineNumbersNums: 1
		#Idx[0]: 19
	#DerivedInfoNums: 0
	.byte	145
	.byte	6
	.byte	5
	.byte	3
	.byte	63
	.byte	252
	.byte	168
	.byte	9
.Lstack_map._CGF8ohos.dep1AE_0:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGF8ohos.dep1BE_0:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGF8ohos.dep1BE_1:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGF8ohos.dep1CE_0:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGF8ohos.dep1DE_0:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGF8ohos.dep1DE_1:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGP8ohos.depiiHv:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGP8ohos.depilHv:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map._CGP8ohos.depirHv:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:4
	.byte	0
	.byte	64
	.byte	34
	.byte	17
	.byte	130
	.byte	4
	.long	.Ltmp71-_CGP8ohos.depirHv
	#[RegIdx: -1, SlotIdx: -1, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 0]
	.byte	0
	.byte	1
	.long	.Ltmp72-_CGP8ohos.depirHv
	#[RegIdx: -1, SlotIdx: -1, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 0]
	.byte	0
	.byte	1
	.long	.Ltmp73-_CGP8ohos.depirHv
	#[RegIdx: -1, SlotIdx: -1, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: -1, SPSlotIdx: 0]
	.byte	0
	.byte	1
	.long	.Ltmp75-_CGP8ohos.depirHv
	#[RegIdx: -1, SlotIdx: -1, LNIdx: -1, DerivedStartIdx: -1, SPRegIdx: 0, SPSlotIdx: -1]
	.byte	64
	.byte	0
	#RegNums: 1
		#Idx[0]: (0x4=4), rcx
	#SlotsNums: 1
		#Idx[0]: BaseOffset: -8, SlotBits: 0x1[ -8 ]
	#LineNumbersNums: 0
	#DerivedInfoNums: 0
	.byte	49
	.byte	12
	.byte	12
	.byte	252
	.byte	16
	.byte	0
.Lstack_map._CGP8ohos.depifHv:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4
.Lstack_map.CJ_MCC_NewObjectFast:
	#StackSize: 0
	#StackmapFormatType: bit map
	#CalleeSaveReg: (0x0=0), offsets(without sign): []
	#StackMapItem nums:0
	.byte	0
	.byte	0
	.byte	4

	.section	.cjmthd,"dw"
.Lmethod_desc._CAN8ohos.dep1AE:
	.long	.Lstack_map._CAN8ohos.dep1AE-.Lmethod_desc._CAN8ohos.dep1AE
	.long	.Lfunc_end0-.Lfunc_begin0
	.long	.Lstr_pool.0-.Lmethod_desc._CAN8ohos.dep1AE
	.long	.Lstr_pool.1-.Lmethod_desc._CAN8ohos.dep1AE
	.long	.Lstr_pool.2-.Lmethod_desc._CAN8ohos.dep1AE
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CAN8ohos.dep1AE
.Ltmp86:
	.long	CJ_except_table0-.Ltmp86
.Lmethod_desc._CAN8ohos.dep1BE:
	.long	.Lstack_map._CAN8ohos.dep1BE-.Lmethod_desc._CAN8ohos.dep1BE
	.long	.Lfunc_end1-.Lfunc_begin1
	.long	.Lstr_pool.3-.Lmethod_desc._CAN8ohos.dep1BE
	.long	.Lstr_pool.1-.Lmethod_desc._CAN8ohos.dep1BE
	.long	.Lstr_pool.2-.Lmethod_desc._CAN8ohos.dep1BE
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CAN8ohos.dep1BE
.Ltmp87:
	.long	CJ_except_table2-.Ltmp87
.Lmethod_desc._CAN8ohos.dep1CE:
	.long	.Lstack_map._CAN8ohos.dep1CE-.Lmethod_desc._CAN8ohos.dep1CE
	.long	.Lfunc_end2-.Lfunc_begin2
	.long	.Lstr_pool.4-.Lmethod_desc._CAN8ohos.dep1CE
	.long	.Lstr_pool.1-.Lmethod_desc._CAN8ohos.dep1CE
	.long	.Lstr_pool.2-.Lmethod_desc._CAN8ohos.dep1CE
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CAN8ohos.dep1CE
.Ltmp88:
	.long	CJ_except_table3-.Ltmp88
.Lmethod_desc._CAN8ohos.dep1DE:
	.long	.Lstack_map._CAN8ohos.dep1DE-.Lmethod_desc._CAN8ohos.dep1DE
	.long	.Lfunc_end3-.Lfunc_begin3
	.long	.Lstr_pool.5-.Lmethod_desc._CAN8ohos.dep1DE
	.long	.Lstr_pool.1-.Lmethod_desc._CAN8ohos.dep1DE
	.long	.Lstr_pool.2-.Lmethod_desc._CAN8ohos.dep1DE
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CAN8ohos.dep1DE
.Ltmp89:
	.long	CJ_except_table4-.Ltmp89
.Lmethod_desc._CGP8ohos.depilHv:
	.long	.Lstack_map._CGP8ohos.depilHv-.Lmethod_desc._CGP8ohos.depilHv
	.long	.Lfunc_end11-.Lfunc_begin11
	.long	.Lstr_pool.6-.Lmethod_desc._CGP8ohos.depilHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depilHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depilHv
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGP8ohos.depilHv
.Ltmp90:
	.long	CJ_except_table12-.Ltmp90
.Lmethod_desc._CGP8ohos.depirHv:
	.long	.Lstack_map._CGP8ohos.depirHv-.Lmethod_desc._CGP8ohos.depirHv
	.long	.Lfunc_end16-.Lfunc_begin16
	.long	.Lstr_pool.8-.Lmethod_desc._CGP8ohos.depirHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depirHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depirHv
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGP8ohos.depirHv
.Ltmp91:
	.long	CJ_except_table17-.Ltmp91
.Lmethod_desc._CGP8ohos.depifHv:
	.long	.Lstack_map._CGP8ohos.depifHv-.Lmethod_desc._CGP8ohos.depifHv
	.long	.Lfunc_end17-.Lfunc_begin17
	.long	.Lstr_pool.9-.Lmethod_desc._CGP8ohos.depifHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depifHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depifHv
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGP8ohos.depifHv
.Ltmp92:
	.long	CJ_except_table18-.Ltmp92
.Lmethod_desc._CGF8ohos.dep1AE_0:
	.long	.Lstack_map._CGF8ohos.dep1AE_0-.Lmethod_desc._CGF8ohos.dep1AE_0
	.long	.Lfunc_end4-.Lfunc_begin4
	.long	.Lstr_pool.10-.Lmethod_desc._CGF8ohos.dep1AE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1AE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1AE_0
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1AE_0
.Ltmp93:
	.long	CJ_except_table5-.Ltmp93
.Lmethod_desc._CGF8ohos.dep1BE_0:
	.long	.Lstack_map._CGF8ohos.dep1BE_0-.Lmethod_desc._CGF8ohos.dep1BE_0
	.long	.Lfunc_end5-.Lfunc_begin5
	.long	.Lstr_pool.11-.Lmethod_desc._CGF8ohos.dep1BE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1BE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1BE_0
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1BE_0
.Ltmp94:
	.long	CJ_except_table6-.Ltmp94
.Lmethod_desc._CGF8ohos.dep1BE_1:
	.long	.Lstack_map._CGF8ohos.dep1BE_1-.Lmethod_desc._CGF8ohos.dep1BE_1
	.long	.Lfunc_end6-.Lfunc_begin6
	.long	.Lstr_pool.12-.Lmethod_desc._CGF8ohos.dep1BE_1
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1BE_1
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1BE_1
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1BE_1
.Ltmp95:
	.long	CJ_except_table7-.Ltmp95
.Lmethod_desc._CGF8ohos.dep1CE_0:
	.long	.Lstack_map._CGF8ohos.dep1CE_0-.Lmethod_desc._CGF8ohos.dep1CE_0
	.long	.Lfunc_end7-.Lfunc_begin7
	.long	.Lstr_pool.13-.Lmethod_desc._CGF8ohos.dep1CE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1CE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1CE_0
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1CE_0
.Ltmp96:
	.long	CJ_except_table8-.Ltmp96
.Lmethod_desc._CGF8ohos.dep1DE_0:
	.long	.Lstack_map._CGF8ohos.dep1DE_0-.Lmethod_desc._CGF8ohos.dep1DE_0
	.long	.Lfunc_end8-.Lfunc_begin8
	.long	.Lstr_pool.14-.Lmethod_desc._CGF8ohos.dep1DE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1DE_0
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1DE_0
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1DE_0
.Ltmp97:
	.long	CJ_except_table9-.Ltmp97
.Lmethod_desc._CGF8ohos.dep1DE_1:
	.long	.Lstack_map._CGF8ohos.dep1DE_1-.Lmethod_desc._CGF8ohos.dep1DE_1
	.long	.Lfunc_end9-.Lfunc_begin9
	.long	.Lstr_pool.15-.Lmethod_desc._CGF8ohos.dep1DE_1
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1DE_1
	.long	.Lstr_pool.7-.Lmethod_desc._CGF8ohos.dep1DE_1
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGF8ohos.dep1DE_1
.Ltmp98:
	.long	CJ_except_table10-.Ltmp98
.Lmethod_desc._CGP8ohos.depiiHv:
	.long	.Lstack_map._CGP8ohos.depiiHv-.Lmethod_desc._CGP8ohos.depiiHv
	.long	.Lfunc_end10-.Lfunc_begin10
	.long	.Lstr_pool.16-.Lmethod_desc._CGP8ohos.depiiHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depiiHv
	.long	.Lstr_pool.7-.Lmethod_desc._CGP8ohos.depiiHv
	.long	.Lstr_pool_dict_offsets-.Lmethod_desc._CGP8ohos.depiiHv
.Ltmp99:
	.long	CJ_except_table11-.Ltmp99
	.section	.cjinitF,"dw"
	.quad	.Lfunc_begin10

	.section	.cjspdct,"dw"
	.p2align	3
	.byte	1
.Lstr_pool_dict_offsets:
	.long	20
	.long	.Lstr_pool_dict.1-.Lstr_pool_dict_offsets
.Lstr_pool_dict.1:
	.ascii	"_CAN,"
	.ascii	"8ohos.dep,"
	.ascii	"1A,"
	.ascii	"E,"
	.ascii	"D:\\code\\intellij\\cangjie\\codex-probes\\hide-mix-equivalent\\src,"
	.ascii	"mix,"
	.ascii	"1.,"
	.ascii	"cj,"
	.ascii	"1B,"
	.ascii	"1C,"
	.ascii	"1D,"
	.ascii	"_CGP,"
	.ascii	"ilHv,"
	.ascii	"irHv,"
	.ascii	"ifHv,"
	.ascii	"_CGF,"
	.ascii	"E_,"
	.ascii	"0,"
	.ascii	"1,"
	.ascii	"iiHv,"
	.byte	0

	.section	.cjsp,"dw"
	.p2align	3
.Lstr_pool.14:
	.asciz	"\020\002\013\021\022"
.Lstr_pool.13:
	.asciz	"\020\002\n\021\022"
.Lstr_pool.11:
	.asciz	"\020\002\t\021\022"
.Lstr_pool.7:
	.byte	0
.Lstr_pool.6:
	.asciz	"\f\002\r"
.Lstr_pool.12:
	.asciz	"\020\002\t\021\023"
.Lstr_pool.9:
	.asciz	"\f\002\017"
.Lstr_pool.8:
	.asciz	"\f\002\016"
.Lstr_pool.5:
	.asciz	"\001\002\013\004"
.Lstr_pool.15:
	.asciz	"\020\002\013\021\023"
.Lstr_pool.10:
	.asciz	"\020\002\003\021\022"
.Lstr_pool.2:
	.asciz	"\006\007\b"
.Lstr_pool.3:
	.asciz	"\001\002\t\004"
.Lstr_pool.1:
	.asciz	"\005"
.Lstr_pool.16:
	.asciz	"\f\002\024"
.Lstr_pool.4:
	.asciz	"\001\002\n\004"
.Lstr_pool.0:
	.asciz	"\001\002\003\004"

	.section	.cjgcrts,"dw"
	.quad	.LRef._CAN8ohos.dep1AE_0
	.quad	.LRef._CAN8ohos.dep1BE_0
	.quad	.LRef._CAN8ohos.dep1BE_1
	.quad	.LRef._CAN8ohos.dep1CE_0
	.quad	.LRef._CAN8ohos.dep1DE_0
	.quad	.LRef._CAN8ohos.dep1DE_1
	.section	.cjti,"dw"
	.globl	"ohos.dep:A.ti"                 # @"ohos.dep:A.ti"
	.p2align	4
".LRef.ohos.dep:A.ti":
"ohos.dep:A.ti":
	.quad	".Lohos.dep:A.name"
	.byte	128                             # 0x80
	.byte	64                              # 0x40
	.short	0                               # 0x0
	.long	0                               # 0x0
	.quad	-9223372036854775808
	.long	0                               # 0x0
	.byte	1                               # 0x1
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	0
	.quad	0
	.quad	0
	.quad	"std.core:Object.ti"
	.quad	0
	.quad	0
	.quad	"ohos.dep:A.ti.reflect"

	.globl	"ohos.dep:B.ti"                 # @"ohos.dep:B.ti"
	.p2align	4
".LRef.ohos.dep:B.ti":
"ohos.dep:B.ti":
	.quad	".Lohos.dep:B.name"
	.byte	128                             # 0x80
	.byte	64                              # 0x40
	.short	0                               # 0x0
	.long	0                               # 0x0
	.quad	-9223372036854775808
	.long	0                               # 0x0
	.byte	1                               # 0x1
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	0
	.quad	0
	.quad	0
	.quad	"std.core:Object.ti"
	.quad	0
	.quad	0
	.quad	"ohos.dep:B.ti.reflect"

	.globl	"ohos.dep:C.ti"                 # @"ohos.dep:C.ti"
	.p2align	4
".LRef.ohos.dep:C.ti":
"ohos.dep:C.ti":
	.quad	".Lohos.dep:C.name"
	.byte	128                             # 0x80
	.byte	64                              # 0x40
	.short	0                               # 0x0
	.long	0                               # 0x0
	.quad	-9223372036854775808
	.long	0                               # 0x0
	.byte	1                               # 0x1
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	0
	.quad	0
	.quad	0
	.quad	"std.core:Object.ti"
	.quad	0
	.quad	0
	.quad	"ohos.dep:C.ti.reflect"

	.globl	"ohos.dep:D.ti"                 # @"ohos.dep:D.ti"
	.p2align	4
".LRef.ohos.dep:D.ti":
"ohos.dep:D.ti":
	.quad	".Lohos.dep:D.name"
	.byte	128                             # 0x80
	.byte	64                              # 0x40
	.short	0                               # 0x0
	.long	0                               # 0x0
	.quad	-9223372036854775808
	.long	0                               # 0x0
	.byte	1                               # 0x1
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	0
	.quad	0
	.quad	0
	.quad	"std.core:Object.ti"
	.quad	0
	.quad	0
	.quad	"ohos.dep:D.ti.reflect"

	.p2align	4                               # @"RawArray<std.core:Object>.ti"
".LRef.RawArray<std.core:Object>.ti":
"RawArray<std.core:Object>.ti":
	.quad	".LRawArray<std.core:Object>.name"
	.byte	130                             # 0x82
	.byte	65                              # 0x41
	.short	0                               # 0x0
	.long	8                               # 0x8
	.quad	0
	.long	0                               # 0x0
	.byte	1                               # 0x1
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	RawArray.tt
	.quad	0
	.quad	0
	.quad	"std.core:Object.ti"
	.quad	0
	.quad	0
	.quad	0

	.p2align	4                               # @"std.core:Array<std.core:Object>.ti"
".LRef.std.core:Array<std.core:Object>.ti":
"std.core:Array<std.core:Object>.ti":
	.quad	".Lstd.core:Array<std.core:Object>.name"
	.byte	22                              # 0x16
	.byte	65                              # 0x41
	.short	3                               # 0x3
	.long	24                              # 0x18
	.quad	-9223372036854775807
	.long	0                               # 0x0
	.byte	8                               # 0x8
	.byte	1                               # 0x1
	.short	32768                           # 0x8000
	.quad	".Lstd.core:Array<std.core:Object>.ti.offsets"
	.quad	"std.core:Array.tt"
	.quad	".Lstd.core:Array<std.core:Object>.ti.typeArgs"
	.quad	".Lstd.core:Array<std.core:Object>.ti.fields"
	.quad	0
	.quad	0
	.quad	0
	.quad	0

	.p2align	4                               # @"RawArray<UInt8>.ti"
".LRef.RawArray<UInt8>.ti":
"RawArray<UInt8>.ti":
	.quad	".LRawArray<UInt8>.name"
	.byte	130                             # 0x82
	.byte	64                              # 0x40
	.short	0                               # 0x0
	.long	0                               # 0x0
	.quad	0
	.long	0                               # 0x0
	.byte	0                               # 0x0
	.byte	0                               # 0x0
	.short	32768                           # 0x8000
	.quad	0
	.quad	RawArray.tt
	.quad	0
	.quad	0
	.quad	UInt8.ti
	.quad	0
	.quad	0
	.quad	0

	.section	.cjfield,"dw"
".LRef..Lstd.core:Array<std.core:Object>.name": # @"std.core:Array<std.core:Object>.name"
".Lstd.core:Array<std.core:Object>.name":
	.asciz	"std.core:Array<std.core:Object>"

	.p2align	4                               # @"std.core:Array<std.core:Object>.ti.fields"
".LRef..Lstd.core:Array<std.core:Object>.ti.fields":
".Lstd.core:Array<std.core:Object>.ti.fields":
	.quad	"RawArray<std.core:Object>.ti"
	.quad	Int64.ti
	.quad	Int64.ti

	.p2align	2                               # @"std.core:Array<std.core:Object>.ti.offsets"
".LRef..Lstd.core:Array<std.core:Object>.ti.offsets":
".Lstd.core:Array<std.core:Object>.ti.offsets":
	.long	0                               # 0x0
	.long	8                               # 0x8
	.long	16                              # 0x10

	.p2align	3                               # @"std.core:Array<std.core:Object>.ti.typeArgs"
".LRef..Lstd.core:Array<std.core:Object>.ti.typeArgs":
".Lstd.core:Array<std.core:Object>.ti.typeArgs":
	.quad	"std.core:Object.ti"

".LRef..LRawArray<std.core:Object>.name": # @"RawArray<std.core:Object>.name"
".LRawArray<std.core:Object>.name":
	.asciz	"RawArray<std.core:Object>"

".LRef..Lohos.dep:D.name":              # @"ohos.dep:D.name"
".Lohos.dep:D.name":
	.asciz	"ohos.dep:D"

".LRef..Lohos.dep:C.name":              # @"ohos.dep:C.name"
".Lohos.dep:C.name":
	.asciz	"ohos.dep:C"

".LRef..Lohos.dep:B.name":              # @"ohos.dep:B.name"
".Lohos.dep:B.name":
	.asciz	"ohos.dep:B"

".LRef..Lohos.dep:A.name":              # @"ohos.dep:A.name"
".Lohos.dep:A.name":
	.asciz	"ohos.dep:A"

".LRef..LRawArray<UInt8>.name":         # @"RawArray<UInt8>.name"
".LRawArray<UInt8>.name":
	.asciz	"RawArray<UInt8>"

	.section	.cjsgt,"dw"
.Lgeneric_ti0:
	.long	".LRef.RawArray<std.core:Object>.ti"-.Lgeneric_ti0
.Lgeneric_ti1:
	.long	".LRef.std.core:Array<std.core:Object>.ti"-.Lgeneric_ti1
	.section	.cjgcflg,"dw"
	.byte	1
	.byte	1
	.byte	1

	.section	.cjrflp,"dw"
	.globl	ohos_ohos.dep.packageInfo       # @ohos_ohos.dep.packageInfo
	.p2align	4
.LRef.ohos_ohos.dep.packageInfo:
ohos_ohos.dep.packageInfo:
	.quad	0
	.long	4                               # 0x4
	.long	0                               # 0x0
	.long	0                               # 0x0
	.long	0                               # 0x0
	.quad	1                               # 0x1
.Ltmp100:
	.quad	.LRef.ohos.reflectStr-.Ltmp100
.Ltmp101:
	.quad	.LRef.ohos.dep.reflectStr-.Ltmp101
.Ltmp102:
	.quad	.LRef.NullString.reflectStr-.Ltmp102
	.quad	0
	.quad	0
	.quad	"ohos.dep:A.ti"
	.quad	"ohos.dep:B.ti"
	.quad	"ohos.dep:C.ti"
	.quad	"ohos.dep:D.ti"

	.section	.cjrflv,"dw"
	.p2align	4                               # @"ohos.dep:A.ti.method0"
".LRef.ohos.dep:A.ti.method0":
"ohos.dep:A.ti.method0":
.Ltmp103:
	.quad	.LRef.init.reflectStr-.Ltmp103
	.long	8                               # 0x8
	.short	0                               # 0x0
	.short	0                               # 0x0
	.quad	"_CN8ohos.dep1A6<init>Hv"
	.quad	Unit.ti
	.quad	0
	.quad	"ohos.dep:A.ti"
	.quad	0
	.quad	0

	.p2align	4                               # @"ohos.dep:A.ti.reflect"
".LRef.ohos.dep:A.ti.reflect":
"ohos.dep:A.ti.reflect":
	.quad	0
	.long	72                              # 0x48
	.short	0                               # 0x0
	.short	0                               # 0x0
	.long	1                               # 0x1
	.long	0                               # 0x0
	.quad	_CAN8ohos.dep1AE
	.quad	0
.Ltmp104:
	.quad	".LRef.ohos.dep:A.ti.method0"-.Ltmp104

	.p2align	4                               # @"ohos.dep:B.ti.method0"
".LRef.ohos.dep:B.ti.method0":
"ohos.dep:B.ti.method0":
.Ltmp105:
	.quad	.LRef.init.reflectStr-.Ltmp105
	.long	8                               # 0x8
	.short	0                               # 0x0
	.short	0                               # 0x0
	.quad	"_CN8ohos.dep1B6<init>Hv"
	.quad	Unit.ti
	.quad	0
	.quad	"ohos.dep:B.ti"
	.quad	0
	.quad	0

	.p2align	4                               # @"ohos.dep:B.ti.reflect"
".LRef.ohos.dep:B.ti.reflect":
"ohos.dep:B.ti.reflect":
	.quad	0
	.long	72                              # 0x48
	.short	0                               # 0x0
	.short	0                               # 0x0
	.long	1                               # 0x1
	.long	0                               # 0x0
	.quad	_CAN8ohos.dep1BE
	.quad	0
.Ltmp106:
	.quad	".LRef.ohos.dep:B.ti.method0"-.Ltmp106

	.p2align	4                               # @"ohos.dep:C.ti.method0"
".LRef.ohos.dep:C.ti.method0":
"ohos.dep:C.ti.method0":
.Ltmp107:
	.quad	.LRef.init.reflectStr-.Ltmp107
	.long	8                               # 0x8
	.short	0                               # 0x0
	.short	0                               # 0x0
	.quad	"_CN8ohos.dep1C6<init>Hv"
	.quad	Unit.ti
	.quad	0
	.quad	"ohos.dep:C.ti"
	.quad	0
	.quad	0

	.p2align	4                               # @"ohos.dep:C.ti.reflect"
".LRef.ohos.dep:C.ti.reflect":
"ohos.dep:C.ti.reflect":
	.quad	0
	.long	72                              # 0x48
	.short	0                               # 0x0
	.short	0                               # 0x0
	.long	1                               # 0x1
	.long	0                               # 0x0
	.quad	_CAN8ohos.dep1CE
	.quad	0
.Ltmp108:
	.quad	".LRef.ohos.dep:C.ti.method0"-.Ltmp108

	.p2align	4                               # @"ohos.dep:D.ti.method0"
".LRef.ohos.dep:D.ti.method0":
"ohos.dep:D.ti.method0":
.Ltmp109:
	.quad	.LRef.init.reflectStr-.Ltmp109
	.long	8                               # 0x8
	.short	0                               # 0x0
	.short	0                               # 0x0
	.quad	"_CN8ohos.dep1D6<init>Hv"
	.quad	Unit.ti
	.quad	0
	.quad	"ohos.dep:D.ti"
	.quad	0
	.quad	0

	.p2align	4                               # @"ohos.dep:D.ti.reflect"
".LRef.ohos.dep:D.ti.reflect":
"ohos.dep:D.ti.reflect":
	.quad	0
	.long	72                              # 0x48
	.short	0                               # 0x0
	.short	0                               # 0x0
	.long	1                               # 0x1
	.long	0                               # 0x0
	.quad	_CAN8ohos.dep1DE
	.quad	0
.Ltmp110:
	.quad	".LRef.ohos.dep:D.ti.method0"-.Ltmp110

.LRef.init.reflectStr:                  # @init.reflectStr
init.reflectStr:
	.asciz	"init"

.LRef.ohos.reflectStr:                  # @ohos.reflectStr
ohos.reflectStr:
	.asciz	"ohos"

.LRef.ohos.dep.reflectStr:              # @ohos.dep.reflectStr
ohos.dep.reflectStr:
	.asciz	"ohos.dep"

.LRef.NullString.reflectStr:            # @NullString.reflectStr
NullString.reflectStr:
	.zero	1


