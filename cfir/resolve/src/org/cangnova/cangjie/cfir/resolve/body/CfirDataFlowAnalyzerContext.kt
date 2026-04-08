package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall

/**
 * 数据流分析上下文。
 *
 * 当前项目尚未接入 Kotlin FIR 那套完整 CFG / smart-cast 引擎，但 body resolve
 * 已经逐步依赖“调用参数进入/退出”“函数调用进入/退出”这类结构化钩子。
 *
 * 这里先对齐这些状态边界，把调用相关的分析栈稳定下来：
 * - 参数解析阶段 frame
 * - 显式接收者解析完成标记
 * - 函数调用 completion 前后的 frame
 *
 * 后续若补齐 CFG、变量赋值分析、smart cast，这些 frame 可以直接作为承载状态的骨架。
 */
class CfirDataFlowAnalyzerContext {
    data class CallArgumentsFrame(
        val call: CfirCall,
        val lambdaArguments: List<CfirAnonymousFunction>,
        var explicitReceiverResolved: Boolean = false,
    )

    data class FunctionCallFrame(
        val functionCall: CfirFunctionCall,
        val lambdaArguments: List<CfirAnonymousFunction>,
        var callCompleted: Boolean = false,
    )

    private val callArgumentsFrames: ArrayDeque<CallArgumentsFrame> = ArrayDeque()
    private val functionCallFrames: ArrayDeque<FunctionCallFrame> = ArrayDeque()

    val currentCallArgumentsFrame: CallArgumentsFrame?
        get() = callArgumentsFrames.lastOrNull()

    val currentFunctionCallFrame: FunctionCallFrame?
        get() = functionCallFrames.lastOrNull()

    fun enterCallArguments(call: CfirCall, lambdaArguments: List<CfirAnonymousFunction>): CallArgumentsFrame {
        return CallArgumentsFrame(
            call = call,
            lambdaArguments = lambdaArguments,
        ).also(callArgumentsFrames::addLast)
    }

    fun exitCallExplicitReceiver() {
        currentCallArgumentsFrame?.explicitReceiverResolved = true
    }

    fun exitCallArguments(): CallArgumentsFrame? {
        return callArgumentsFrames.removeLastOrNull()
    }

    fun enterFunctionCall(functionCall: CfirFunctionCall, lambdaArguments: List<CfirAnonymousFunction>): FunctionCallFrame {
        return FunctionCallFrame(
            functionCall = functionCall,
            lambdaArguments = lambdaArguments,
        ).also(functionCallFrames::addLast)
    }

    fun exitFunctionCall(functionCall: CfirFunctionCall, callCompleted: Boolean): FunctionCallFrame? {
        val frame = functionCallFrames.removeLastOrNull() ?: return null
        check(frame.functionCall === functionCall) {
            "Unbalanced function-call data-flow frame: expected ${frame.functionCall::class.simpleName}, got ${functionCall::class.simpleName}"
        }
        frame.callCompleted = callCompleted
        return frame
    }

    fun reset() {
        callArgumentsFrames.clear()
        functionCallFrames.clear()
    }
}
