package org.cangnova.cangjie.chir.core.reference

import org.cangnova.cangjie.chir.core.testkit.ChirDiffEntry
import org.cangnova.cangjie.chir.core.testkit.ChirDiffReportFormatter
import org.cangnova.cangjie.chir.core.testkit.ChirDiffStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChirDiffReportFormatTest {

    @Test
    fun `diff report renders pass fail and accepted records in stable format`() {
        val report = ChirDiffReportFormatter.render(
            listOf(
                ChirDiffEntry(caseId = "b-case", status = ChirDiffStatus.FAIL, summary = "missing block"),
                ChirDiffEntry(caseId = "a-case", status = ChirDiffStatus.PASS, summary = "ok"),
                ChirDiffEntry(caseId = "c-case", status = ChirDiffStatus.ACCEPTED_DIFF, summary = "enum order differs"),
            ),
        )

        val expected = """
CHIR-DIFF-REPORT
total=3 pass=1 fail=1 accepted=1
PASS|a-case|ok
FAIL|b-case|missing block
ACCEPTED_DIFF|c-case|enum order differs
        """.trimIndent()

        assertEquals(expected, report)
    }
}
