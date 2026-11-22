#ifndef COSMIUM_MATH_H
#define COSMIUM_MATH_H

#include <string>

namespace cosmium::math {

std::string cosmosSystemInstruction();

std::string sanitizeAndFormatMath(
    const std::string& rawText,
    int standardGrade,
    const std::string& streamId
);

std::string calibrateCosmosPrompt(
    const std::string& userQuery,
    int standardGrade,
    const std::string& streamId
);

std::string buildMarkdownWithProtectedMathHtml(const std::string& rawText);

std::string formatReadableMathFallback(const std::string& rawText);

std::string formatAsciiFormulaToLatex(const std::string& rawFormula);

} // namespace cosmium::math

#endif // COSMIUM_MATH_H
