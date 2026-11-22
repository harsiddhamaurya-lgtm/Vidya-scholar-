#ifndef COSMIUM_RAG_H
#define COSMIUM_RAG_H

#include <string>
#include <vector>

namespace cosmium::rag {

std::vector<std::string> tokenizeText(const std::string& raw);

std::string buildSafeFtsMatchQuery(const std::string& rawQuery);

std::vector<float> encodeConceptVector(const std::string& text, int dim);

std::vector<float> encodeWeightedDocumentVector(
    const std::string& topicTitle,
    const std::string& subjectName,
    const std::string& formulaTags,
    const std::string& contentText,
    int dim
);

float computeTopicSpecificityScore(
    const std::string& query,
    const std::string& topicTitle,
    const std::string& subjectName,
    const std::string& formulaTags,
    const std::string& contentText,
    float denseSim,
    float ftsScore,
    float bm25Score
);

std::string composeFallbackTextbookResponse(
    const std::string& topicTitle,
    const std::string& subjectName,
    int chapterNumber,
    const std::string& contentText,
    const std::string& formulaTags,
    const std::string& workedExample,
    bool lowRamNotice
);

float cosineSimilarity(const float* a, const float* b, int length);

bool evaluateQuizAnswer(
    const std::string& studentRaw,
    const std::string& canonicalRaw,
    const std::string& keywordsPipe
);

} // namespace cosmium::rag

#endif // COSMIUM_RAG_H
