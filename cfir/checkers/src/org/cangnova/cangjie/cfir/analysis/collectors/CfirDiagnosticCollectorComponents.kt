package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.CfirReportCommitterDiagnosticComponent

/**
 * 璇婃柇鏀堕泦缁勪欢瀹瑰櫒銆? *
 * 瀵归綈 K2 `DiagnosticCollectorComponents`銆? *
 * @param regularComponents 甯歌妫€鏌ョ粍浠讹紙澹版槑妫€鏌ュ櫒銆佽〃杈惧紡妫€鏌ュ櫒绛夛級
 * @param reportCommitter   璇婃柇鎻愪氦缁勪欢锛屽湪姣忎釜鍏冪礌妫€鏌ュ畬鎴愬悗鎻愪氦 pending 璇婃柇
 */
class CfirDiagnosticCollectorComponents(
    val regularComponents: Array<AbstractDiagnosticCollectorComponent>,
    val reportCommitter: CfirReportCommitterDiagnosticComponent,
)

