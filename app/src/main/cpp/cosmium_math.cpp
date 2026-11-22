#include "cosmium_math.h"

#include <algorithm>
#include <cctype>
#include <utility>
#include <vector>

namespace cosmium::math {
namespace {

void replaceAll(std::string& str, const std::string& from, const std::string& to) {
    if (from.empty()) return;
    size_t pos = 0;
    while ((pos = str.find(from, pos)) != std::string::npos) {
        str.replace(pos, from.length(), to);
        pos += to.length();
    }
}

std::string trimAscii(const std::string& s) {
    size_t start = 0;
    while (start < s.size() && std::isspace(static_cast<unsigned char>(s[start]))) {
        ++start;
    }
    size_t end = s.size();
    while (end > start && std::isspace(static_cast<unsigned char>(s[end - 1]))) {
        --end;
    }
    return s.substr(start, end - start);
}

std::string toLowerAscii(const std::string& s) {
    std::string out = s;
    for (char& ch : out) {
        ch = static_cast<char>(std::tolower(static_cast<unsigned char>(ch)));
    }
    return out;
}

bool startsWithAt(const std::string& s, size_t pos, const std::string& prefix) {
    return pos + prefix.size() <= s.size() && s.compare(pos, prefix.size(), prefix) == 0;
}

bool isWordBoundaryChar(char ch) {
    unsigned char uc = static_cast<unsigned char>(ch);
    return !std::isalnum(uc) && ch != '_';
}

std::string escapeHtmlText(const std::string& input) {
    std::string out;
    out.reserve(input.size() + 16);
    for (char c : input) {
        switch (c) {
            case '&': out += "&amp;"; break;
            case '<': out += "&lt;"; break;
            case '>': out += "&gt;"; break;
            default: out.push_back(c); break;
        }
    }
    return out;
}

void replaceFencedMathBlocks(std::string& text) {
    static const char* kTags[] = {"```latex", "```tex", "```math"};
    size_t pos = 0;
    while (pos < text.size()) {
        size_t found = text.find("```", pos);
        if (found == std::string::npos) break;

        size_t matchedTagLen = 0;
        for (const char* tag : kTags) {
            size_t tagLen = std::char_traits<char>::length(tag);
            if (found + tagLen <= text.size()) {
                std::string candidate = toLowerAscii(text.substr(found, tagLen));
                if (candidate == tag) {
                    size_t after = found + tagLen;
                    while (after < text.size() && (text[after] == ' ' || text[after] == '\t')) {
                        ++after;
                    }
                    if (after < text.size() && text[after] == '\n') {
                        matchedTagLen = (after + 1) - found;
                        break;
                    }
                }
            }
        }

        if (matchedTagLen == 0) {
            pos = found + 3;
            continue;
        }

        size_t closePos = text.find("```", found + matchedTagLen);
        if (closePos == std::string::npos) break;

        std::string inner = trimAscii(text.substr(found + matchedTagLen, closePos - (found + matchedTagLen)));
        std::string replacement = "\n\n$$\n" + inner + "\n$$\n\n";
        text.replace(found, (closePos + 3) - found, replacement);
        pos = found + replacement.size();
    }
}

void cleanHeadingAndStrayTokens(std::string& text) {
    replaceAll(text, "###/", "");
    replaceAll(text, "##/", "");
    replaceAll(text, "#/", "");

    replaceAll(text, "###$$", "###\n\n$$");
    replaceAll(text, "##$$", "##\n\n$$");
    replaceAll(text, "#$$", "#\n\n$$");
    replaceAll(text, "$$###", "$$\n\n###");
    replaceAll(text, "$$##", "$$\n\n##");
    replaceAll(text, "$$#", "$$\n\n#");

    replaceAll(text, std::string("\f") + "rac", "\\frac");
    replaceAll(text, std::string("\f") + "orall", "\\forall");
    replaceAll(text, std::string("\t") + "ext", "\\text");
    replaceAll(text, std::string("\t") + "imes", "\\times");
    replaceAll(text, std::string("\t") + "heta", "\\theta");
    replaceAll(text, std::string("\t") + "an", "\\tan");
    replaceAll(text, std::string("\t") + "au", "\\tau");
    replaceAll(text, std::string("\t") + "o", "\\to");
    replaceAll(text, std::string("\b") + "egin", "\\begin");
    replaceAll(text, std::string("\b") + "ar", "\\bar");
    replaceAll(text, std::string("\b") + "eta", "\\beta");
    replaceAll(text, std::string("\b") + "inom", "\\binom");
    replaceAll(text, std::string("\r") + "ight", "\\right");
    replaceAll(text, std::string("\r") + "ho", "\\rho");
}

void applyMacroFallbacks(std::string& text) {
    replaceAll(text, "\\tfrac", "\\frac");
    replaceAll(text, "\\dfrac", "\\frac");
    replaceAll(text, "\\cfrac", "\\frac");
    replaceAll(text, "\\bm{", "\\mathbf{");
    replaceAll(text, "\\mathbbm{", "\\mathbf{");
    replaceAll(text, "\\implies", "\\Rightarrow");
    replaceAll(text, "\\iff", "\\Leftrightarrow");
    replaceAll(text, "\\impliedby", "\\Leftarrow");
    replaceAll(text, "\\celsius", "^\\circ\\mathrm{C}");
    replaceAll(text, "\\degree", "^\\circ");
    replaceAll(text, "\\ohm", "\\Omega");
    replaceAll(text, "\\angstrom", "\\text{\\AA}");
}

void normalizeDisplayDelimiters(std::string& text) {
    replaceAll(text, "\r\n", "\n");
    replaceAll(text, "\\[", "\n\n$$\n");
    replaceAll(text, "\\]", "\n$$\n\n");
    replaceAll(text, "\\(", "$");
    replaceAll(text, "\\)", "$");
    replaceFencedMathBlocks(text);
}

void balanceMathDelimiters(std::string& text) {
    size_t displayCount = 0;
    size_t pos = 0;
    while ((pos = text.find("$$", pos)) != std::string::npos) {
        displayCount++;
        pos += 2;
    }
    if (displayCount % 2 != 0) {
        text += "\n$$\n";
    }

    size_t loneDollarCount = 0;
    for (size_t i = 0; i < text.size(); ++i) {
        if (text[i] == '$') {
            if (i + 1 < text.size() && text[i + 1] == '$') {
                ++i;
            } else {
                loneDollarCount++;
            }
        }
    }
    if (loneDollarCount % 2 != 0) {
        text += "$";
    }

    if (!text.empty() && text.back() == '\\') {
        text.pop_back();
    }
}

size_t findClosingDoubleDollar(const std::string& text, size_t startIndex) {
    size_t idx = startIndex;
    while (idx < text.size()) {
        size_t found = text.find("$$", idx);
        if (found == std::string::npos) return std::string::npos;
        if (found == 0 || text[found - 1] != '\\') return found;
        idx = found + 2;
    }
    return std::string::npos;
}

size_t findClosingSingleDollar(const std::string& text, size_t startIndex) {
    size_t idx = startIndex;
    while (idx < text.size()) {
        char ch = text[idx];
        if (ch == '\n') return std::string::npos;
        if (ch == '$' && (idx == 0 || text[idx - 1] != '\\')) {
            if (idx + 1 < text.size() && text[idx + 1] == '$') {
                return std::string::npos;
            }
            return idx;
        }
        ++idx;
    }
    return std::string::npos;
}

size_t matchBareLatexCommand(const std::string& text, size_t pos) {
    if (pos >= text.size() || text[pos] != '\\') return 0;

    static const char* kArgCmds[] = {
        "\\overrightarrow", "\\frac", "\\sqrt", "\\overline", "\\mathbf",
        "\\mathrm", "\\text", "\\vec", "\\hat", "\\bar"
    };
    for (const char* cmd : kArgCmds) {
        size_t cmdLen = std::char_traits<char>::length(cmd);
        if (startsWithAt(text, pos, cmd)) {
            size_t cur = pos + cmdLen;
            while (cur < text.size() && std::isspace(static_cast<unsigned char>(text[cur]))) ++cur;
            bool hasArgs = false;
            while (cur < text.size() && (text[cur] == '{' || text[cur] == '[')) {
                char openCh = text[cur];
                char closeCh = (openCh == '{') ? '}' : ']';
                size_t scan = cur + 1;
                int depth = 1;
                while (scan < text.size() && depth > 0) {
                    if (text[scan] == openCh) depth++;
                    else if (text[scan] == closeCh) depth--;
                    ++scan;
                }
                if (depth != 0) break;
                hasArgs = true;
                cur = scan;
                while (cur < text.size() && std::isspace(static_cast<unsigned char>(text[cur]))) ++cur;
            }
            if (hasArgs) {
                return cur - pos;
            }
        }
    }

    static const char* kSymCmds[] = {
        "\\rightleftharpoons", "\\leftrightarrow", "\\rightarrow", "\\leftarrow",
        "\\partial", "\\approx", "\\epsilon", "\\lambda", "\\nabla", "\\infty",
        "\\alpha", "\\theta", "\\omega", "\\sigma", "\\equiv", "\\times",
        "\\delta", "\\gamma", "\\prod", "\\beta", "\\cdot", "\\int",
        "\\sum", "\\lim", "\\rho", "\\phi", "\\neq", "\\div",
        "\\pi", "\\mu", "\\le", "\\ge", "\\pm"
    };
    for (const char* cmd : kSymCmds) {
        size_t cmdLen = std::char_traits<char>::length(cmd);
        if (startsWithAt(text, pos, cmd)) {
            size_t endPos = pos + cmdLen;
            if (endPos == text.size() || isWordBoundaryChar(text[endPos])) {
                return cmdLen;
            }
        }
    }
    return 0;
}

std::string formatInlineMarkdown(const std::string& rawLine) {
    std::string escaped = escapeHtmlText(rawLine);

    auto replaceDelimitedTag = [&](const std::string& delim, const std::string& openTag, const std::string& closeTag, bool allowNewline) {
        std::string result;
        result.reserve(escaped.size());
        size_t i = 0;
        while (i < escaped.size()) {
            if (startsWithAt(escaped, i, delim)) {
                size_t close = escaped.find(delim, i + delim.size());
                if (close != std::string::npos && close > i + delim.size()) {
                    std::string inner = escaped.substr(i + delim.size(), close - (i + delim.size()));
                    if (allowNewline || inner.find('\n') == std::string::npos) {
                        result += openTag + inner + closeTag;
                        i = close + delim.size();
                        continue;
                    }
                }
            }
            result.push_back(escaped[i]);
            ++i;
        }
        escaped = std::move(result);
    };

    replaceDelimitedTag("`", "<code>", "</code>", false);
    replaceDelimitedTag("**", "<strong>", "</strong>", true);
    replaceDelimitedTag("*", "<em>", "</em>", false);
    return escaped;
}

bool isDisplayPlaceholderLine(const std::string& line) {
    const std::string prefix = "@@COSMOS_DISP_";
    if (!startsWithAt(line, 0, prefix) || line.size() <= prefix.size() + 2) return false;
    if (!startsWithAt(line, line.size() - 2, "@@")) return false;
    for (size_t i = prefix.size(); i < line.size() - 2; ++i) {
        if (!std::isdigit(static_cast<unsigned char>(line[i]))) return false;
    }
    return true;
}

bool isUnorderedListLine(const std::string& line, std::string& outContent) {
    if (startsWithAt(line, 0, "- ") || startsWithAt(line, 0, "* ")) {
        outContent = trimAscii(line.substr(2));
        return true;
    }
    if (startsWithAt(line, 0, "• ")) {
        outContent = trimAscii(line.substr(std::char_traits<char>::length("• ")));
        return true;
    }
    return false;
}

bool isOrderedListLine(const std::string& line, std::string& outContent) {
    size_t i = 0;
    while (i < line.size() && std::isdigit(static_cast<unsigned char>(line[i]))) ++i;
    if (i > 0 && i + 1 < line.size() && line[i] == '.' && line[i + 1] == ' ') {
        outContent = trimAscii(line.substr(i + 2));
        return true;
    }
    return false;
}

void replaceBracedCommand(std::string& text, const std::string& cmd, const std::string& prefix, const std::string& suffix) {
    size_t pos = 0;
    std::string needle = cmd + "{";
    while ((pos = text.find(needle, pos)) != std::string::npos) {
        size_t braceStart = pos + needle.size();
        size_t braceEnd = text.find('}', braceStart);
        if (braceEnd == std::string::npos) {
            pos = braceStart;
            continue;
        }
        std::string inner = text.substr(braceStart, braceEnd - braceStart);
        if (inner.find('{') != std::string::npos) {
            pos = braceStart;
            continue;
        }
        std::string replacement = prefix + inner + suffix;
        text.replace(pos, (braceEnd + 1) - pos, replacement);
        pos += replacement.size();
    }
}

void replaceParenFunction(std::string& text, const std::string& fnName, const std::string& prefix, const std::string& suffix) {
    size_t pos = 0;
    std::string needle = fnName + "(";
    while ((pos = text.find(needle, pos)) != std::string::npos) {
        if (pos > 0 && !isWordBoundaryChar(text[pos - 1])) {
            pos += needle.size();
            continue;
        }
        size_t parenStart = pos + needle.size();
        size_t parenEnd = text.find(')', parenStart);
        if (parenEnd == std::string::npos) {
            pos = parenStart;
            continue;
        }
        std::string inner = text.substr(parenStart, parenEnd - parenStart);
        if (inner.find('(') != std::string::npos) {
            pos = parenStart;
            continue;
        }
        std::string replacement = prefix + inner + suffix;
        text.replace(pos, (parenEnd + 1) - pos, replacement);
        pos += replacement.size();
    }
}

void replaceFracCommands(std::string& text) {
    static const char* kCmds[] = {"\\frac{", "\\tfrac{", "\\dfrac{"};
    for (int pass = 0; pass < 3; ++pass) {
        bool changed = false;
        for (const char* needle : kCmds) {
            size_t needleLen = std::char_traits<char>::length(needle);
            size_t pos = 0;
            while ((pos = text.find(needle, pos)) != std::string::npos) {
                size_t numStart = pos + needleLen;
                size_t numEnd = text.find('}', numStart);
                if (numEnd == std::string::npos || numEnd + 1 >= text.size() || text[numEnd + 1] != '{') {
                    pos = numStart;
                    continue;
                }
                size_t denStart = numEnd + 2;
                size_t denEnd = text.find('}', denStart);
                if (denEnd == std::string::npos) {
                    pos = numStart;
                    continue;
                }
                std::string num = text.substr(numStart, numEnd - numStart);
                std::string den = text.substr(denStart, denEnd - denStart);
                if (num.find('{') != std::string::npos || den.find('{') != std::string::npos) {
                    pos = numStart;
                    continue;
                }
                std::string replacement = "(" + num + ") / (" + den + ")";
                text.replace(pos, (denEnd + 1) - pos, replacement);
                pos += replacement.size();
                changed = true;
            }
        }
        if (!changed) break;
    }
}

} // namespace

std::string cosmosSystemInstruction() {
    return "You are Cosmium, the AI study mentor for Vidhya Scholar aligned with the latest rationalized NCERT & NCF-SE competency-based syllabus.\n"
           "Follow these strict pedagogical, anti-hallucination, and formatting rules for every answer:\n"
           "1. Strict Topic Purity & Latest NCERT Fidelity: Focus strictly on the exact concept, theorem, historical date, or problem the student asked about. "
           "Follow the latest rationalized NCERT syllabus (never confuse students with dropped chapters, and give deep conceptual clarity suited to modern competency/application questions). Never mix formulas or examples from unrelated neighboring topics.\n"
           "2. Interleaved Textbook Structure (Never Dump Formulas Together): Alternate explanation sentences with equations:\n"
           "   - Open with a clear definition and 2-3 intuitive takeaways.\n"
           "   - '### 1. Mathematical Formulation' (or Core Rules / Milestones): Introduce the core equation or rule in prose, show it alone in $$...$$, add a '**where:**' bullet list explaining every symbol with $...$, then explain any general/integral form in prose + $$...$$.\n"
           "   - '### 2. Why It Works' (or Derivation / Cause & Logic): Walk through each logical step alternating prose and $$...$$.\n"
           "   - '### 3. Crucial Nuances & Common Pitfalls': 3-4 concept-specific bullets on scope limits, boundary conditions, and exam traps.\n"
           "   - '### 4. Quick Worked Example': Solve one focused competency-style example step by step with prose + $$...$$.\n"
           "3. KaTeX Delimiters: Always wrap inline variables and symbols in $...$ (e.g. $W_{\\text{net}}$, $\\vec{v}$). "
           "Place every standalone equation, derivation step, and final result on its own $$...$$ block with blank lines before and after. Never wrap math inside ``` code fences.";
}

std::string sanitizeAndFormatMath(
    const std::string& rawText,
    int /*standardGrade*/,
    const std::string& /*streamId*/
) {
    std::string text = rawText;
    cleanHeadingAndStrayTokens(text);
    applyMacroFallbacks(text);
    normalizeDisplayDelimiters(text);
    balanceMathDelimiters(text);
    return text;
}

std::string calibrateCosmosPrompt(
    const std::string& userQuery,
    int standardGrade,
    const std::string& streamId
) {
    std::string prompt;
    prompt += "You are Cosmium—the encouraging AI study sanctuary and doubt solver for Vidhya Scholar (Latest Rationalized NCERT Syllabus).\n";

    if (standardGrade <= 10) {
        prompt += "[Grade Calibration: New Competency NCERT Standard " + std::to_string(standardGrade) + "]\n";
        prompt += "- Subjects: Mathematics, Science (Physics, Chemistry, Biology), Social Science (History, Political Science, Economics, Geography).\n";
        prompt += "- Conceptual Rigor: New NCERT Class 9-10 textbooks emphasize deep understanding, proof intuition, assertion-reason traps, and multi-step word problems rather than rote plugging. Build reasoning step by step.\n";
        prompt += "- Typography & Math: Use clean, standard LaTeX for all formulas, balanced chemical equations (e.g. \\text{2H}_2 + \\text{O}_2 \\rightarrow \\text{2H}_2\\text{O}), algebra, coordinate geometry, and statistics.\n";
    } else {
        prompt += "[Grade Calibration: Latest NCERT Senior Secondary Standard " + std::to_string(standardGrade) + " • Stream: " + streamId + "]\n";
        if (streamId == "science_pcm") {
            prompt += "- Subjects: Physics, Chemistry, Mathematics, Computer Science.\n";
            prompt += "- Typography: Use calibrated KaTeX + extended LaTeX hybrid for deep NCERT derivations, calculus (\\int, \\frac{d}{dx}, \\lim), vectors (\\vec{v}), matrices (\\begin{bmatrix}), electromagnetism, thermodynamics, and chemical kinetics/electrochemistry.\n";
        } else if (streamId == "science_pcb") {
            prompt += "- Subjects: Physics, Chemistry, Biology (Genetics, Cell Biology, Physiology, Biotechnology).\n";
            prompt += "- Typography: Use calibrated KaTeX for physical chemistry kinetics/equilibrium, genetics Hardy-Weinberg equations ($p^2 + 2pq + q^2 = 1$), and physics problems.\n";
        } else if (streamId == "commerce") {
            prompt += "- Subjects: Accountancy, Micro/Macro Economics, Business Studies, Applied/Core Mathematics.\n";
            prompt += "- Typography: Use calibrated KaTeX for Accountancy (accounting equations, ratios, goodwill, depreciation), Economics multipliers ($k = \\frac{1}{1 - \\text{MPC}}$), and Business Studies principles.\n";
        } else if (streamId == "arts") {
            prompt += "- Subjects: History (verified NCERT dates & events), Political Science (constitutional articles & federalism), Economics, Humanities.\n";
            prompt += "- Typography: Use structured timelines, exact constitutional article citations, and clean KaTeX for economic indices.\n";
        } else {
            prompt += "- Typography: Use calibrated KaTeX + LaTeX hybrid for all formulas, derivations, and calculations.\n";
        }
    }

    prompt += "\n[Strict Pedagogical & Anti-Hallucination Rules]\n";
    prompt += "1. Stay 100% On Topic: Answer only what the student asked. Do not pull unrelated formulas (for example, do not include friction or inclined-plane decomposition when asked solely about the Work-Energy Theorem). Only use reference context if it directly matches the question.\n";
    prompt += "2. Never Group Equations Into One Dump: Always explain a line in prose, show the centered $$...$$ equation, and list variable definitions under **where:** using $...$ before moving to the next form or derivation step.\n";
    prompt += "3. Consistent Textbook Sections: Use ### headings (1. Mathematical Formulation, 2. Why It Works / Derivation, 3. Crucial Nuances & Common Pitfalls, 4. Quick Worked Example) adapted naturally to the subject.\n";
    prompt += "4. Clean KaTeX Math Delimiters: Wrap all inline variables/units in $...$ and place every major equation or calculation step on its own $$...$$ line with blank lines before and after. Preserve all LaTeX backslashes (\\frac, \\sqrt, \\vec, \\int, \\Delta, \\text) and never put math inside ``` code fences.\n";
    prompt += "\nStudent Question: " + userQuery;

    return prompt;
}

std::string buildMarkdownWithProtectedMathHtml(const std::string& rawText) {
    std::string normalized = rawText;
    normalizeDisplayDelimiters(normalized);

    std::vector<std::pair<bool, std::string>> mathSlots;
    std::string withPlaceholders;
    withPlaceholders.reserve(normalized.size());

    size_t i = 0;
    const size_t len = normalized.size();
    while (i < len) {
        if (normalized[i] == '$' && (i == 0 || normalized[i - 1] != '\\')) {
            if (i + 1 < len && normalized[i + 1] == '$') {
                size_t close = findClosingDoubleDollar(normalized, i + 2);
                if (close != std::string::npos) {
                    std::string inner = trimAscii(normalized.substr(i + 2, close - (i + 2)));
                    size_t slotIdx = mathSlots.size();
                    mathSlots.emplace_back(true, inner);
                    withPlaceholders += "\n@@COSMOS_DISP_" + std::to_string(slotIdx) + "@@\n";
                    i = close + 2;
                    continue;
                }
            } else {
                size_t close = findClosingSingleDollar(normalized, i + 1);
                if (close != std::string::npos) {
                    std::string inner = trimAscii(normalized.substr(i + 1, close - (i + 1)));
                    size_t slotIdx = mathSlots.size();
                    mathSlots.emplace_back(false, inner);
                    withPlaceholders += "@@COSMOS_INL_" + std::to_string(slotIdx) + "@@";
                    i = close + 1;
                    continue;
                }
            }
        }
        withPlaceholders.push_back(normalized[i]);
        ++i;
    }

    // Wrap stray bare LaTeX commands outside protected slots
    std::string withAutoWrapped;
    withAutoWrapped.reserve(withPlaceholders.size());
    size_t k = 0;
    while (k < withPlaceholders.size()) {
        if (withPlaceholders[k] == '\\') {
            size_t matchLen = matchBareLatexCommand(withPlaceholders, k);
            if (matchLen > 0) {
                size_t slotIdx = mathSlots.size();
                mathSlots.emplace_back(false, trimAscii(withPlaceholders.substr(k, matchLen)));
                withAutoWrapped += "@@COSMOS_INL_" + std::to_string(slotIdx) + "@@";
                k += matchLen;
                continue;
            }
        }
        withAutoWrapped.push_back(withPlaceholders[k]);
        ++k;
    }

    // Split into lines and compile non-math Markdown
    std::vector<std::string> lines;
    size_t lineStart = 0;
    while (lineStart <= withAutoWrapped.size()) {
        size_t nl = withAutoWrapped.find('\n', lineStart);
        if (nl == std::string::npos) {
            lines.push_back(withAutoWrapped.substr(lineStart));
            break;
        }
        lines.push_back(withAutoWrapped.substr(lineStart, nl - lineStart));
        lineStart = nl + 1;
    }

    std::string htmlOut;
    bool inUl = false;
    bool inOl = false;
    std::vector<std::string> paragraphLines;

    auto flushParagraph = [&]() {
        if (!paragraphLines.empty()) {
            htmlOut += "<p>";
            for (size_t idx = 0; idx < paragraphLines.size(); ++idx) {
                if (idx > 0) htmlOut += "<br/>";
                htmlOut += formatInlineMarkdown(paragraphLines[idx]);
            }
            htmlOut += "</p>\n";
            paragraphLines.clear();
        }
    };

    auto closeLists = [&]() {
        if (inUl) {
            htmlOut += "</ul>\n";
            inUl = false;
        }
        if (inOl) {
            htmlOut += "</ol>\n";
            inOl = false;
        }
    };

    for (const auto& rawLine : lines) {
        std::string trimmed = trimAscii(rawLine);
        std::string listContent;
        if (trimmed.empty()) {
            flushParagraph();
            closeLists();
        } else if (isDisplayPlaceholderLine(trimmed)) {
            flushParagraph();
            closeLists();
            htmlOut += "<div class=\"math-display-block\">" + trimmed + "</div>\n";
        } else if (startsWithAt(trimmed, 0, "#### ")) {
            flushParagraph();
            closeLists();
            htmlOut += "<h4>" + formatInlineMarkdown(trimAscii(trimmed.substr(5))) + "</h4>\n";
        } else if (startsWithAt(trimmed, 0, "### ")) {
            flushParagraph();
            closeLists();
            htmlOut += "<h3>" + formatInlineMarkdown(trimAscii(trimmed.substr(4))) + "</h3>\n";
        } else if (startsWithAt(trimmed, 0, "## ")) {
            flushParagraph();
            closeLists();
            htmlOut += "<h2>" + formatInlineMarkdown(trimAscii(trimmed.substr(3))) + "</h2>\n";
        } else if (startsWithAt(trimmed, 0, "# ")) {
            flushParagraph();
            closeLists();
            htmlOut += "<h1>" + formatInlineMarkdown(trimAscii(trimmed.substr(2))) + "</h1>\n";
        } else if (startsWithAt(trimmed, 0, "> ")) {
            flushParagraph();
            closeLists();
            htmlOut += "<blockquote>" + formatInlineMarkdown(trimAscii(trimmed.substr(2))) + "</blockquote>\n";
        } else if (isUnorderedListLine(trimmed, listContent)) {
            flushParagraph();
            if (inOl) {
                htmlOut += "</ol>\n";
                inOl = false;
            }
            if (!inUl) {
                htmlOut += "<ul>\n";
                inUl = true;
            }
            htmlOut += "<li>" + formatInlineMarkdown(listContent) + "</li>\n";
        } else if (isOrderedListLine(trimmed, listContent)) {
            flushParagraph();
            if (inUl) {
                htmlOut += "</ul>\n";
                inUl = false;
            }
            if (!inOl) {
                htmlOut += "<ol>\n";
                inOl = true;
            }
            htmlOut += "<li>" + formatInlineMarkdown(listContent) + "</li>\n";
        } else {
            closeLists();
            paragraphLines.push_back(trimmed);
        }
    }
    flushParagraph();
    closeLists();

    for (size_t idx = 0; idx < mathSlots.size(); ++idx) {
        std::string safeLatex = escapeHtmlText(mathSlots[idx].second);
        if (mathSlots[idx].first) {
            replaceAll(htmlOut, "@@COSMOS_DISP_" + std::to_string(idx) + "@@", "$$\n" + safeLatex + "\n$$");
        } else {
            replaceAll(htmlOut, "@@COSMOS_INL_" + std::to_string(idx) + "@@", "$" + safeLatex + "$");
        }
    }
    return htmlOut;
}

std::string formatReadableMathFallback(const std::string& rawText) {
    std::string out = rawText;
    replaceAll(out, "\r\n", "\n");

    // Strip fenced math tags and code fences
    size_t fencePos = 0;
    while ((fencePos = out.find("```", fencePos)) != std::string::npos) {
        size_t lineEnd = fencePos + 3;
        while (lineEnd < out.size() && out[lineEnd] != '\n' && !std::isspace(static_cast<unsigned char>(out[lineEnd]))) {
            ++lineEnd;
        }
        std::string tag = toLowerAscii(out.substr(fencePos + 3, lineEnd - (fencePos + 3)));
        if (tag == "latex" || tag == "tex" || tag == "math") {
            while (lineEnd < out.size() && std::isspace(static_cast<unsigned char>(out[lineEnd]))) {
                ++lineEnd;
            }
            out.erase(fencePos, lineEnd - fencePos);
        } else {
            out.erase(fencePos, 3);
        }
    }

    replaceAll(out, "$$", "");
    replaceAll(out, "$", "");
    replaceAll(out, "\\[", "");
    replaceAll(out, "\\]", "");
    replaceAll(out, "\\(", "");
    replaceAll(out, "\\)", "");
    replaceAll(out, "\\left", "");
    replaceAll(out, "\\right", "");
    replaceAll(out, "\\quad", "  ");
    replaceAll(out, "\\,", " ");

    replaceBracedCommand(out, "\\text", "", "");
    replaceBracedCommand(out, "\\mathrm", "", "");
    replaceBracedCommand(out, "\\mathbf", "", "");
    replaceBracedCommand(out, "\\vec", "", "\xE2\x83\x97"); // combining right arrow U+20D7
    replaceBracedCommand(out, "\\sqrt", "\xE2\x88\x9A(", ")"); // √(...)

    replaceFracCommands(out);

    replaceAll(out, "\\rightleftharpoons", "\xE2\x87\x8C");
    replaceAll(out, "\\leftrightarrow", "\xE2\x86\x94");
    replaceAll(out, "\\rightarrow", "\xE2\x86\x92");
    replaceAll(out, "\\times", "\xC3\x97");
    replaceAll(out, "\\cdot", "\xC2\xB7");
    replaceAll(out, "\\pm", "\xC2\xB1");
    replaceAll(out, "\\le", "\xE2\x89\xA4");
    replaceAll(out, "\\ge", "\xE2\x89\xA5");
    replaceAll(out, "\\neq", "\xE2\x89\xA0");
    replaceAll(out, "\\approx", "\xE2\x89\x88");
    replaceAll(out, "\\infty", "\xE2\x88\x9E");
    replaceAll(out, "\\int", "\xE2\x88\xAB");
    replaceAll(out, "\\sum", "\xE2\x88\x91");
    replaceAll(out, "\\pi", "\xCF\x80");
    replaceAll(out, "\\theta", "\xCE\xB8");
    replaceAll(out, "\\alpha", "\xCE\xB1");
    replaceAll(out, "\\beta", "\xCE\xB2");
    replaceAll(out, "\\delta", "\xCE\x94");
    replaceAll(out, "\\Omega", "\xCE\xA9");
    replaceAll(out, "**", "");

    // Strip leading markdown headings (# .. ####) line by line
    std::string cleaned;
    cleaned.reserve(out.size());
    size_t pos = 0;
    while (pos <= out.size()) {
        size_t nl = out.find('\n', pos);
        std::string line = (nl == std::string::npos) ? out.substr(pos) : out.substr(pos, nl - pos);
        size_t scan = 0;
        while (scan < line.size() && std::isspace(static_cast<unsigned char>(line[scan]))) ++scan;
        size_t hashes = 0;
        while (scan + hashes < line.size() && line[scan + hashes] == '#' && hashes < 4) ++hashes;
        if (hashes > 0 && scan + hashes < line.size() && line[scan + hashes] == ' ') {
            line = line.substr(scan + hashes + 1);
        }
        cleaned += line;
        if (nl == std::string::npos) break;
        cleaned.push_back('\n');
        pos = nl + 1;
    }

    return trimAscii(cleaned);
}

std::string formatAsciiFormulaToLatex(const std::string& rawFormula) {
    std::string f = trimAscii(rawFormula);
    if (f.size() >= 4 && startsWithAt(f, 0, "$$") && startsWithAt(f, f.size() - 2, "$$")) {
        return trimAscii(f.substr(2, f.size() - 4));
    }
    if (f.size() >= 2 && f.front() == '$' && f.back() == '$') {
        return trimAscii(f.substr(1, f.size() - 2));
    }

    std::string trailingNote;
    static const char* kNotes[] = {"(mirror)", "(lens)", "(constant)", "(at STP)", "(per unit)"};
    std::string lowerF = toLowerAscii(f);
    for (const char* note : kNotes) {
        std::string lowerNote = toLowerAscii(note);
        if (lowerF.size() >= lowerNote.size() &&
            lowerF.compare(lowerF.size() - lowerNote.size(), lowerNote.size(), lowerNote) == 0) {
            std::string rawNote = f.substr(f.size() - lowerNote.size() + 1, lowerNote.size() - 2);
            trailingNote = " \\quad \\text{(" + rawNote + ")}";
            f = trimAscii(f.substr(0, f.size() - lowerNote.size()));
            break;
        }
    }

    replaceAll(f, "+-", "\\pm ");
    replaceAll(f, ">=", "\\ge ");
    replaceAll(f, "<=", "\\le ");
    replaceAll(f, "!=", "\\neq ");
    replaceAll(f, "<->", "\\rightleftharpoons ");
    replaceAll(f, "->", "\\rightarrow ");
    replaceAll(f, "(1/2)", "\\frac{1}{2}");
    replaceAll(f, "(1)/(2)", "\\frac{1}{2}");
    replaceAll(f, "(1) / (2)", "\\frac{1}{2}");
    replaceAll(f, "*", " \\cdot ");
    replaceParenFunction(f, "sqrt", "\\sqrt{", "}");
    replaceParenFunction(f, "nth_root", "\\sqrt[n]{", "}");

    // Structured Greek delta/subscript shorthands before snake_case text fallback
    replaceAll(f, "Delta_G0", "\\Delta G^\\circ");
    replaceAll(f, "Delta_ng", "\\Delta n_g");
    replaceAll(f, "Delta_", "\\Delta ");
    replaceAll(f, "E0_cell", "E^\\circ_{\\text{cell}}");
    replaceAll(f, "W_net", "W_{\\text{net}}");
    replaceAll(f, "F_net", "F_{\\text{net}}");
    replaceAll(f, "f_max", "f_{\\max}");
    replaceAll(f, "H_max", "H_{\\max}");
    replaceAll(f, "K_max", "K_{\\max}");
    replaceAll(f, "f_res", "f_{\\text{res}}");
    replaceAll(f, "mu_s", "\\mu_s");
    replaceAll(f, "mu_k", "\\mu_k");
    replaceAll(f, "phi_0", "\\phi_0");
    replaceAll(f, "t_1/2", "t_{1/2}");

    // Convert Greek letter words on word boundaries
    static const char* kGreeks[] = {
        "epsilon", "lambda", "theta", "alpha", "gamma", "delta",
        "omega", "sigma", "beta", "rho", "phi", "mu", "pi"
    };
    for (const char* greek : kGreeks) {
        size_t gLen = std::char_traits<char>::length(greek);
        size_t pos = 0;
        while ((pos = f.find(greek, pos)) != std::string::npos) {
            bool leftOk = (pos == 0) || (isWordBoundaryChar(f[pos - 1]) && f[pos - 1] != '\\');
            bool rightOk = (pos + gLen == f.size()) || isWordBoundaryChar(f[pos + gLen]);
            if (leftOk && rightOk) {
                f.replace(pos, gLen, std::string("\\") + greek);
                pos += gLen + 1;
            } else {
                pos += gLen;
            }
        }
    }

    // Convert snake_case descriptive identifiers into \text{...} (skipping single-letter/math subscripts like a_c, v_i, K_f)
    std::string withTextLabels;
    withTextLabels.reserve(f.size() + 16);
    size_t idx = 0;
    while (idx < f.size()) {
        unsigned char ch = static_cast<unsigned char>(f[idx]);
        if (std::isalpha(ch) && (idx == 0 || f[idx - 1] != '\\')) {
            size_t end = idx + 1;
            size_t underscorePos = std::string::npos;
            while (end < f.size()) {
                unsigned char ec = static_cast<unsigned char>(f[end]);
                if (std::isalnum(ec) || f[end] == '_') {
                    if (f[end] == '_' && underscorePos == std::string::npos) underscorePos = end;
                    ++end;
                } else {
                    break;
                }
            }
            if (underscorePos != std::string::npos && end > idx + 2 && f[end - 1] != '_') {
                size_t stemLen = underscorePos - idx;
                size_t subLen = end - (underscorePos + 1);
                if (stemLen >= 2 && subLen >= 2) {
                    std::string label = f.substr(idx, end - idx);
                    replaceAll(label, "_", " ");
                    withTextLabels += "\\text{" + label + "}";
                    idx = end;
                    continue;
                }
            }
        }
        withTextLabels.push_back(f[idx]);
        ++idx;
    }
    f = std::move(withTextLabels);

    // Format quadratic root numerator/denominator
    size_t quadPos = f.find("(-b \\pm \\sqrt{");
    if (quadPos != std::string::npos) {
        size_t closeParen = f.find(")/2a", quadPos);
        if (closeParen == std::string::npos) closeParen = f.find(") / 2a", quadPos);
        if (closeParen != std::string::npos) {
            std::string inner = f.substr(quadPos + 1, closeParen - (quadPos + 1));
            size_t tailEnd = (f.compare(closeParen, 4, ")/2a") == 0) ? (closeParen + 4) : (closeParen + 6);
            f.replace(quadPos, tailEnd - quadPos, "\\frac{" + inner + "}{2a}");
        }
    }

    return trimAscii(f + trailingNote);
}

} // namespace cosmium::math
