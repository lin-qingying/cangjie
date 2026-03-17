package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionStage

/**
 * 璋冪敤绉嶇被锛屽喅瀹氬€欓€夐獙璇佺绾夸娇鐢ㄥ摢浜涢樁娈点€? *
 * 姣忕璋冪敤绉嶇被鎼哄甫涓€涓獙璇侀樁娈靛簭鍒?[resolutionSequence]锛? * [CfirResolutionStageRunner] 鎸夊簭鎵ц杩欎簺闃舵銆? *
 * 瀵归綈 K2 CallKind锛岀畝鍖栦负 3 绉嶏紙鍘绘帀 DelegatingConstructorCall/CustomForIde 绛夛級銆? */
sealed class CfirCallKind {

    /** 璇ヨ皟鐢ㄧ绫绘墍闇€鐨勯獙璇侀樁娈靛簭鍒?*/
    abstract val resolutionSequence: List<CfirResolutionStage>

    /** 鍑芥暟璋冪敤 */
    class Function(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 鍙橀噺/灞炴€ц闂?*/
    class VariableAccess(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 鏋勯€犲櫒璋冪敤 */
    class ConstructorCall(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()
}

