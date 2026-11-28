(() => {
    "use strict";

    const expectedMarker = "=====预期======";
    const actualMarker = "=====得到======";

    function parseComparison(rawText) {
        const text = rawText.replace(/\r\n?/g, "\n");
        const expectedMarkerAt = text.indexOf(expectedMarker);
        const actualMarkerAt = text.indexOf(actualMarker, expectedMarkerAt + expectedMarker.length);
        if (expectedMarkerAt < 0 || actualMarkerAt < 0) return null;

        let expected = text.slice(expectedMarkerAt + expectedMarker.length, actualMarkerAt);
        let actual = text.slice(actualMarkerAt + actualMarker.length);
        if (expected.startsWith("\n")) expected = expected.slice(1);
        if (expected.endsWith("\n")) expected = expected.slice(0, -1);
        if (actual.startsWith("\n")) actual = actual.slice(1);

        const stackTraceAt = actual.search(/\n[\t ]+at [\w.$]+\(/);
        if (stackTraceAt >= 0) actual = actual.slice(0, stackTraceAt);
        if (actual.endsWith("\n")) actual = actual.slice(0, -1);
        return { expected, actual };
    }

    /** 使用 Myers 逐行算法，适合测试快照中少量行变更的比较场景。 */
    function diffLines(expectedText, actualText) {
        const expected = expectedText.split("\n");
        const actual = actualText.split("\n");
        const maxDistance = expected.length + actual.length;
        const offset = maxDistance + 1;
        const frontier = new Int32Array(maxDistance * 2 + 3);
        frontier.fill(-1);
        frontier[offset + 1] = 0;
        const trace = [];
        let solvedDistance = -1;

        for (let distance = 0; distance <= maxDistance; distance++) {
            let solved = false;
            for (let diagonal = -distance; diagonal <= distance; diagonal += 2) {
                let x;
                if (
                    diagonal === -distance ||
                    (diagonal !== distance && frontier[offset + diagonal - 1] < frontier[offset + diagonal + 1])
                ) {
                    x = frontier[offset + diagonal + 1];
                } else {
                    x = frontier[offset + diagonal - 1] + 1;
                }

                let y = x - diagonal;
                while (x < expected.length && y < actual.length && expected[x] === actual[y]) {
                    x++;
                    y++;
                }
                frontier[offset + diagonal] = x;

                if (x >= expected.length && y >= actual.length) {
                    solved = true;
                    solvedDistance = distance;
                    break;
                }
            }
            trace.push(frontier.slice());
            if (solved) break;
        }

        let x = expected.length;
        let y = actual.length;
        const operations = [];
        for (let distance = solvedDistance; distance > 0; distance--) {
            const previousFrontier = trace[distance - 1];
            const diagonal = x - y;
            const previousDiagonal =
                diagonal === -distance ||
                (diagonal !== distance && previousFrontier[offset + diagonal - 1] < previousFrontier[offset + diagonal + 1])
                    ? diagonal + 1
                    : diagonal - 1;
            const previousX = previousFrontier[offset + previousDiagonal];
            const previousY = previousX - previousDiagonal;

            while (x > previousX && y > previousY) {
                operations.push({ type: "equal", text: expected[x - 1] });
                x--;
                y--;
            }
            if (x === previousX) {
                y--;
                operations.push({ type: "added", text: actual[y] });
            } else {
                x--;
                operations.push({ type: "removed", text: expected[x] });
            }
        }

        while (x > 0 && y > 0) {
            operations.push({ type: "equal", text: expected[x - 1] });
            x--;
            y--;
        }
        while (x > 0) {
            x--;
            operations.push({ type: "removed", text: expected[x] });
        }
        while (y > 0) {
            y--;
            operations.push({ type: "added", text: actual[y] });
        }
        return operations.reverse();
    }

    function makeDiffView(comparison) {
        const operations = diffLines(comparison.expected, comparison.actual);
        const removedCount = operations.filter(({ type }) => type === "removed").length;
        const addedCount = operations.filter(({ type }) => type === "added").length;
        const view = document.createElement("section");
        view.className = "cj-failure-diff";
        view.setAttribute("aria-label", "Expected and actual test output diff");

        const summary = document.createElement("div");
        summary.className = "cj-failure-diff-summary";
        if (removedCount === 0 && addedCount === 0) {
            const identical = document.createElement("span");
            identical.className = "cj-failure-diff-identical";
            identical.textContent = "The expected and actual text are identical.";
            summary.append(identical);
        } else {
            const removed = document.createElement("span");
            removed.className = "cj-failure-diff-count cj-failure-diff-count--removed";
            removed.textContent = `− 预期 ${removedCount} 行`;
            const added = document.createElement("span");
            added.className = "cj-failure-diff-count cj-failure-diff-count--added";
            added.textContent = `+ 实际 ${addedCount} 行`;
            summary.append(removed, added);
        }
        view.append(summary);

        const lines = document.createElement("div");
        lines.className = "cj-failure-diff-lines";
        lines.setAttribute("role", "list");
        let expectedLine = 1;
        let actualLine = 1;
        for (const operation of operations) {
            const row = document.createElement("div");
            row.className = `cj-failure-diff-line cj-failure-diff-line--${operation.type}`;
            row.setAttribute("role", "listitem");

            const expectedNumber = document.createElement("span");
            expectedNumber.className = "cj-failure-diff-number";
            expectedNumber.textContent = operation.type === "added" ? "" : String(expectedLine++);
            const actualNumber = document.createElement("span");
            actualNumber.className = "cj-failure-diff-number";
            actualNumber.textContent = operation.type === "removed" ? "" : String(actualLine++);
            const marker = document.createElement("span");
            marker.className = "cj-failure-diff-marker";
            marker.textContent = operation.type === "removed" ? "−" : operation.type === "added" ? "+" : " ";
            const content = document.createElement("code");
            content.className = "cj-failure-diff-content";
            content.textContent = operation.text.length === 0 ? "\u00a0" : operation.text;
            row.append(expectedNumber, actualNumber, marker, content);
            lines.append(row);
        }
        view.append(lines);
        return view;
    }

    function enhanceFailureDetails() {
        for (const heading of document.querySelectorAll("h3")) {
            if (heading.textContent.trim() !== "Failure details") continue;
            const rawView = heading.nextElementSibling;
            const pre = rawView?.matches(".code") ? rawView.querySelector("pre") : null;
            if (!pre) continue;

            const comparison = parseComparison(pre.textContent || "");
            if (!comparison) continue;

            const toolbar = document.createElement("div");
            toolbar.className = "cj-failure-diff-toolbar";
            toolbar.setAttribute("role", "group");
            toolbar.setAttribute("aria-label", "Failure detail views");

            const title = document.createElement("span");
            title.className = "cj-failure-diff-title";
            title.textContent = "预期 ↔ 实际";
            const controls = document.createElement("div");
            controls.className = "cj-failure-diff-controls";
            const diffButton = document.createElement("button");
            diffButton.className = "cj-failure-diff-button";
            diffButton.type = "button";
            diffButton.textContent = "彩色 Diff";
            diffButton.setAttribute("aria-pressed", "true");
            const rawButton = document.createElement("button");
            rawButton.className = "cj-failure-diff-button";
            rawButton.type = "button";
            rawButton.textContent = "原始详情";
            rawButton.setAttribute("aria-pressed", "false");
            controls.append(diffButton, rawButton);
            toolbar.append(title, controls);

            const diffView = makeDiffView(comparison);
            rawView.classList.add("cj-failure-diff-raw");
            rawView.hidden = true;
            diffButton.addEventListener("click", () => {
                diffView.hidden = false;
                rawView.hidden = true;
                diffButton.setAttribute("aria-pressed", "true");
                rawButton.setAttribute("aria-pressed", "false");
            });
            rawButton.addEventListener("click", () => {
                diffView.hidden = true;
                rawView.hidden = false;
                diffButton.setAttribute("aria-pressed", "false");
                rawButton.setAttribute("aria-pressed", "true");
            });

            heading.after(toolbar, diffView);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", enhanceFailureDetails, { once: true });
    } else {
        enhanceFailureDetails();
    }
})();
