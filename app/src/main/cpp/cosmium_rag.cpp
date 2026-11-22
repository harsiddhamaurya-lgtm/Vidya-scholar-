#include "cosmium_rag.h"
#include "cosmium_math.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdint>
#include <unordered_set>

namespace cosmium::rag {
namespace {

std::string toLowerAscii(const std::string& input) {
    std::string out;
    out.reserve(input.size());
    for (unsigned char ch : input) {
        out.push_back(static_cast<char>(std::tolower(ch)));
    }
    return out;
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

void replaceAllAscii(std::string& str, const std::string& from, const std::string& to) {
    if (from.empty()) return;
    size_t pos = 0;
    while ((pos = str.find(from, pos)) != std::string::npos) {
        str.replace(pos, from.length(), to);
        pos += to.length();
    }
}

int32_t javaStringHashCode(const std::string& s) {
    uint32_t h = 0;
    for (unsigned char ch : s) {
        h = h * 31u + static_cast<uint32_t>(ch);
    }
    return static_cast<int32_t>(h);
}

bool isTokenChar(char ch) {
    unsigned char c = static_cast<unsigned char>(ch);
    return std::isalnum(c) != 0 || c == '_';
}

bool isBridgeChar(char ch) {
    return ch == '+' || ch == '^' || ch == '=' || ch == '-';
}

const std::unordered_set<std::string>& stopWords() {
    static const std::unordered_set<std::string> kStop = {
        "the", "is", "are", "was", "were", "a", "an", "and", "or", "of", "to", "in",
        "for", "on", "with", "by", "from", "at", "as", "that", "this", "what", "how",
        "why", "when", "which", "who", "please", "explain", "solve", "tell", "me", "my",
        "can", "you", "it", "be", "into", "using", "about", "define", "state", "derive",
        "give", "example", "describe", "does"
    };
    return kStop;
}

const std::unordered_set<std::string>& genericAcademicModifiers() {
    static const std::unordered_set<std::string> kGeneric = {
        "law", "laws", "theorem", "rule", "rules", "equation", "equations", "formula",
        "formulas", "principle", "principles", "concept", "theory", "method", "methods",
        "effect", "process", "processes", "model", "first", "second", "third", "general",
        "basic", "work", "energy", "force", "rate", "change", "value", "system", "physics",
        "chemistry", "math", "mathematics", "biology", "science", "economics", "accountancy",
        "business", "history", "political", "social", "chapter", "topic", "class", "question"
    };
    return kGeneric;
}

struct ConceptGroup {
    const char* root;
    std::vector<const char*> anchors;
};

// 58 fine-grained, non-overlapping NCERT topic clusters spanning Class 9-12 & all streams
const std::vector<ConceptGroup>& conceptGroups() {
    static const std::vector<ConceptGroup> kGroups = {
        // Mechanics & Physics
        {"work_energy", {"work_energy", "workenergy", "kinetic", "w_net", "wnet", "delta_k"}},
        {"friction", {"friction", "mu_s", "mu_k", "repose", "limiting", "static_friction", "rough"}},
        {"impulse_momentum", {"impulse", "momentum", "collision", "delta_p", "recoil", "conservation_momentum"}},
        {"circular_motion", {"centripetal", "centrifugal", "banking", "angular", "a_c"}},
        {"newton_laws", {"newton", "inertia", "fbd", "free_body", "action_reaction", "f_eq_ma"}},
        {"kinematics", {"kinematics", "uniformly", "accelerated", "displacement", "velocity", "v=u+at"}},
        {"projectile_motion", {"projectile", "trajectory", "parabolic", "h_max", "horizontal_range"}},
        {"gravitation", {"gravitation", "gravity", "kepler", "escape_velocity", "orbital_velocity", "buoyancy", "archimedes"}},
        {"thermodynamics", {"thermodynamics", "thermo", "entropy", "enthalpy", "isothermal", "adiabatic", "pv_eq_nrt", "internal_energy"}},
        // Optics & Electromagnetism
        {"optics_mirror", {"mirror", "concave", "convex_mirror", "magnification", "pole", "focal"}},
        {"optics_refraction", {"refraction", "snell", "lens", "dioptre", "refractive", "refractive_index"}},
        {"ohm_circuits", {"ohm", "resistor", "resistance", "resistivity", "v_eq_ir", "series", "parallel"}},
        {"joule_heating", {"joule_heating", "electric_power", "heat_dissipated", "watt", "i2rt"}},
        {"kirchhoff_wheatstone", {"kirchhoff", "kcl", "kvl", "wheatstone", "junction", "loop", "drift"}},
        {"emi_faraday", {"faraday", "lenz", "flux", "induced", "emf", "magnetic_flux"}},
        {"ac_impedance", {"impedance", "lcr", "resonance", "inductive", "capacitive", "phasor", "alternating"}},
        {"modern_photoelectric", {"photoelectric", "work_function", "stopping", "photon", "einstein", "threshold_frequency"}},
        {"bohr_spectrum", {"rydberg", "balmer", "lyman", "paschen", "hydrogen_spectrum"}},
        // Mathematics (Class 9-12)
        {"real_numbers", {"rational", "irrational", "surd", "rationalize", "conjugate", "hcf", "lcm", "fundamental_theorem"}},
        {"exponents_surds", {"exponent", "exponents", "nth_root", "power_law"}},
        {"polynomials_identities", {"polynomial", "identity", "identities", "factorization", "remainder", "factor_theorem"}},
        {"linear_equations", {"linear_equations", "substitution", "elimination", "intersecting", "coincident", "consistent"}},
        {"coordinate_geometry", {"coordinate", "distance_formula", "midpoint", "section_formula", "cartesian", "collinear", "abscissa"}},
        {"heron_triangles", {"heron", "semiperimeter", "triangle", "congruence", "thales", "bpt", "proportionality"}},
        {"quadratic", {"quadratic", "discriminant", "roots", "parabola", "vieta"}},
        {"ap_series", {"arithmetic_progression", "progression", "common_difference", "nth_term", "s_n", "a_n"}},
        {"trigonometry", {"trigonometry", "trigonometric", "sine", "cosine", "pythagorean", "heights", "elevation"}},
        {"derivative", {"derivative", "differentiation", "power_rule", "product_rule", "tangent", "slope", "d_dx"}},
        {"limit", {"limit", "continuity", "approaches", "indeterminate", "squeeze"}},
        {"integral", {"integral", "integration", "antiderivative", "substitution", "ilate", "definite"}},
        {"matrices_determinants", {"matrix", "matrices", "determinant", "inverse_matrix", "cofactor", "adjoint"}},
        {"probability_bayes", {"probability", "bayes", "conditional", "independent_events", "random_variable"}},
        // Chemistry & Biology
        {"chemical_reactions", {"balancing", "combination", "decomposition", "displacement", "rancidity", "corrosion", "exothermic"}},
        {"mole_concept", {"mole", "avogadro", "molar", "stoichiometry", "6.022"}},
        {"atomic_orbital", {"quantum", "orbital", "bohr", "pauli", "spin", "broglie", "subshell"}},
        {"redox", {"redox", "oxidation", "reduction", "oxidizing", "reducing"}},
        {"bonding_vsepr", {"vsepr", "hybridization", "tetrahedral", "sp3", "sp2", "lone_pair"}},
        {"chemical_equilibrium", {"equilibrium", "chatelier", "kp", "kc", "ph", "buffer"}},
        {"electrochemistry", {"nernst", "electrode", "galvanic", "e0_cell", "gibbs"}},
        {"kinetics_chem", {"kinetics", "rate_constant", "first_order", "half_life", "arrhenius"}},
        {"life_processes", {"nutrition", "respiration", "aerobic", "anaerobic", "pyruvate", "nephron", "xylem", "phloem"}},
        {"cell_mitosis", {"mitosis", "meiosis", "cell_cycle", "fluid_mosaic", "chromosome", "ploidy"}},
        {"genetics_dna", {"dna", "rna", "replication", "okazaki", "mendel", "hardy", "operon"}},
        {"photosynthesis_bio", {"photosynthesis", "chloroplast", "calvin", "atp", "nadph", "light_reaction"}},
        // Coding & CS
        {"python_algo", {"python", "recursion", "binary_search", "complexity", "memoization", "stack"}},
        // Commerce (Accountancy, Economics, Business Studies)
        {"accounting_rules", {"debit", "credit", "ledger", "journal", "assets", "liabilities", "double_entry", "drawings"}},
        {"depreciation_acc", {"depreciation", "straight_line", "written_down", "slm", "wdv", "salvage"}},
        {"accounting_ratios", {"liquidity", "current_ratio", "quick_ratio", "solvency", "acid_test"}},
        {"goodwill_valuation", {"goodwill", "super_profit", "capital_employed", "normal_profit", "years_purchase"}},
        {"micro_utility", {"marginal_utility", "diminishing", "consumer_equilibrium", "satiety", "total_utility"}},
        {"micro_elasticity", {"elasticity", "price_elasticity", "inelastic", "unitary", "e_d"}},
        {"macro_gdp", {"gdp", "nnp", "nfia", "national_income", "expenditure_method", "factor_cost"}},
        {"macro_multiplier", {"multiplier", "mpc", "mps", "keynes", "autonomous_investment", "paradox_thrift"}},
        {"business_mgmt", {"fayol", "taylor", "management", "planning", "organizing", "controlling", "unity_command", "scalar_chain"}},
        // Social Science, History & Political Science
        {"constitution_rights", {"constitution", "fundamental_rights", "habeas", "mandamus", "article", "writ", "preamble"}},
        {"indian_history", {"champaran", "satyagraha", "dandi", "non_cooperation", "constituent", "gandhi", "quit_india", "rowlatt"}},
        {"french_revolution_civics", {"french_revolution", "bastille", "robespierre", "jacobean", "estates_general"}},
        {"federalism_polity", {"federalism", "concurrent_list", "union_list", "state_list", "panchayati", "decentralisation"}}
    };
    return kGroups;
}

std::unordered_set<std::string> detectActiveConcepts(const std::vector<std::string>& tokens) {
    std::unordered_set<std::string> active;
    const auto& groups = conceptGroups();
    for (const auto& tok : tokens) {
        for (const auto& group : groups) {
            if (tok == group.root || tok.find(group.root) != std::string::npos) {
                active.insert(group.root);
                continue;
            }
            for (const char* anchor : group.anchors) {
                std::string a(anchor);
                if (tok == a || (a.size() >= 4 && tok.find(a) != std::string::npos)) {
                    active.insert(group.root);
                    break;
                }
            }
        }
    }
    return active;
}

void projectTextIntoVector(
    std::vector<float>& vec,
    int vecDim,
    const std::string& text,
    float fieldWeight
) {
    if (text.empty() || fieldWeight <= 0.0f) return;
    auto tokens = tokenizeText(text);
    if (tokens.empty()) return;

    auto addFeature = [&](const std::string& feature, float weight) {
        int32_t h1 = javaStringHashCode(feature);
        int idx1 = static_cast<int>((static_cast<uint32_t>(h1) & 0x7FFFFFFFu) % static_cast<uint32_t>(vecDim));
        float sign1 = ((static_cast<uint32_t>(h1) >> 31) == 0u) ? 1.0f : -1.0f;
        vec[idx1] += sign1 * weight;

        int32_t h2 = javaStringHashCode(feature + "_c2");
        int idx2 = static_cast<int>((static_cast<uint32_t>(h2) & 0x7FFFFFFFu) % static_cast<uint32_t>(vecDim));
        float sign2 = ((static_cast<uint32_t>(h2) >> 31) == 0u) ? 1.0f : -1.0f;
        vec[idx2] += sign2 * weight * 0.65f;

        int32_t h3 = javaStringHashCode(feature + "_r3");
        int idx3 = static_cast<int>((static_cast<uint32_t>(h3) & 0x7FFFFFFFu) % static_cast<uint32_t>(vecDim));
        float sign3 = ((static_cast<uint32_t>(h3) >> 31) == 0u) ? 1.0f : -1.0f;
        vec[idx3] += sign3 * weight * 0.40f;
    };

    const auto& groups = conceptGroups();
    const auto& generic = genericAcademicModifiers();
    for (const auto& tok : tokens) {
        // IDF-inspired term weighting: specific domain nouns dominate over generic academic modifiers
        float idfFactor = (generic.find(tok) != generic.end()) ? 0.25f : 1.35f;
        addFeature(tok, idfFactor * fieldWeight);

        if (tok.size() >= 5) {
            size_t limit = std::min<size_t>(tok.size(), 16);
            for (size_t i = 0; i + 3 <= limit; ++i) {
                addFeature("c3_" + tok.substr(i, 3), 0.20f * idfFactor * fieldWeight);
            }
            for (size_t i = 0; i + 4 <= limit; ++i) {
                addFeature("c4_" + tok.substr(i, 4), 0.26f * idfFactor * fieldWeight);
            }
        }

        for (const auto& group : groups) {
            bool exact = (tok == group.root);
            bool anchorHit = false;
            if (!exact) {
                for (const char* anchor : group.anchors) {
                    std::string a(anchor);
                    if (tok == a) {
                        exact = true;
                        break;
                    }
                    if (a.size() >= 4 && tok.find(a) != std::string::npos) {
                        anchorHit = true;
                    }
                }
            }
            if (exact) {
                addFeature(std::string("cpt_") + group.root, 1.45f * fieldWeight);
            } else if (anchorHit) {
                addFeature(std::string("cpt_") + group.root, 0.65f * fieldWeight);
            }
        }
    }

    for (size_t i = 0; i + 1 < tokens.size(); ++i) {
        addFeature("bg_" + tokens[i] + "_" + tokens[i + 1], 0.95f * fieldWeight);
    }
}

void l2Normalize(std::vector<float>& vec) {
    double sumSq = 0.0;
    for (float v : vec) {
        sumSq += static_cast<double>(v) * static_cast<double>(v);
    }
    float norm = static_cast<float>(std::sqrt(sumSq));
    if (norm > 1e-6f) {
        for (float& v : vec) {
            v /= norm;
        }
    }
}

std::string normalizeSpeechAndMath(const std::string& raw) {
    std::string s = toLowerAscii(raw);
    replaceAllAscii(s, "equal to", " = ");
    replaceAllAscii(s, "equals", " = ");
    replaceAllAscii(s, "plus", " + ");
    replaceAllAscii(s, "minus", " - ");
    replaceAllAscii(s, "multiplied by", " * ");
    replaceAllAscii(s, "times", " * ");
    replaceAllAscii(s, "divided by", " / ");
    replaceAllAscii(s, "squared", "^2");
    replaceAllAscii(s, "cubed", "^3");

    std::string collapsed;
    collapsed.reserve(s.size());
    bool prevSpace = true;
    for (char c : s) {
        if (std::isspace(static_cast<unsigned char>(c))) {
            if (!prevSpace) {
                collapsed.push_back(' ');
                prevSpace = true;
            }
        } else {
            collapsed.push_back(c);
            prevSpace = false;
        }
    }
    if (!collapsed.empty() && collapsed.back() == ' ') {
        collapsed.pop_back();
    }
    return collapsed;
}

std::string stripSpacesAndParens(const std::string& s) {
    std::string out;
    out.reserve(s.size());
    for (char c : s) {
        if (!std::isspace(static_cast<unsigned char>(c)) && c != '(' && c != ')') {
            out.push_back(c);
        }
    }
    return out;
}

} // namespace

std::vector<std::string> tokenizeText(const std::string& raw) {
    std::string normalized = toLowerAscii(raw);
    replaceAllAscii(normalized, "work-energy", " work_energy work energy kinetic ");
    replaceAllAscii(normalized, "work energy", " work_energy work energy kinetic ");
    replaceAllAscii(normalized, "arithmetic progression", " arithmetic_progression ap common_difference ");
    replaceAllAscii(normalized, "fundamental theorem of arithmetic", " fundamental_theorem real_numbers hcf lcm ");
    replaceAllAscii(normalized, "linear equations", " linear_equations substitution elimination consistent ");
    replaceAllAscii(normalized, "distance formula", " distance_formula coordinate_geometry ");
    replaceAllAscii(normalized, "section formula", " section_formula coordinate_geometry ");
    replaceAllAscii(normalized, "heron's formula", " heron semiperimeter triangle ");
    replaceAllAscii(normalized, "heron formula", " heron semiperimeter triangle ");
    replaceAllAscii(normalized, "bayes theorem", " bayes conditional probability ");
    replaceAllAscii(normalized, "bayes' theorem", " bayes conditional probability ");
    replaceAllAscii(normalized, "fundamental rights", " fundamental_rights constitution article writs ");
    replaceAllAscii(normalized, "french revolution", " french_revolution bastille estates_general ");
    replaceAllAscii(normalized, "estates general", " estates_general french_revolution ");
    replaceAllAscii(normalized, "concurrent list", " concurrent_list federalism union_list state_list ");
    replaceAllAscii(normalized, "current ratio", " current_ratio liquidity quick_ratio ");
    replaceAllAscii(normalized, "quick ratio", " quick_ratio liquidity acid_test ");
    replaceAllAscii(normalized, "super profit", " super_profit goodwill capital_employed ");
    replaceAllAscii(normalized, "price elasticity", " price_elasticity elasticity demand ");
    replaceAllAscii(normalized, "marginal utility", " marginal_utility consumer_equilibrium ");
    replaceAllAscii(normalized, "investment multiplier", " multiplier mpc mps macroeconomics ");
    replaceAllAscii(normalized, "straight line", " straight_line depreciation slm ");
    replaceAllAscii(normalized, "written down", " written_down depreciation wdv ");
    replaceAllAscii(normalized, "w_net", " w_net work_energy ");
    replaceAllAscii(normalized, "f=ma", " f_eq_ma force mass acceleration newton ");
    replaceAllAscii(normalized, "v=ir", " v_eq_ir ohm voltage current resistance ");
    replaceAllAscii(normalized, "e=mc^2", " e_eq_mc2 energy relativity ");
    replaceAllAscii(normalized, "d/dx", " d_dx derivative differentiation calculus ");
    replaceAllAscii(normalized, "pv=nrt", " pv_eq_nrt ideal gas constant thermodynamics ");
    replaceAllAscii(normalized, "a=l+c", " accounting equation assets liabilities capital ");

    std::vector<std::string> tokens;
    const auto& stops = stopWords();
    size_t n = normalized.size();
    size_t i = 0;
    while (i < n) {
        if (!isTokenChar(normalized[i])) {
            ++i;
            continue;
        }
        size_t start = i;
        while (i < n && isTokenChar(normalized[i])) {
            ++i;
        }
        if (i + 1 < n && isBridgeChar(normalized[i]) && isTokenChar(normalized[i + 1])) {
            ++i;
            while (i < n && isTokenChar(normalized[i])) {
                ++i;
            }
        }
        std::string tok = normalized.substr(start, i - start);
        if (tok.size() >= 2 && stops.find(tok) == stops.end()) {
            tokens.push_back(std::move(tok));
        }
    }
    return tokens;
}

std::string buildSafeFtsMatchQuery(const std::string& rawQuery) {
    auto tokens = tokenizeText(rawQuery);
    std::vector<std::string> cleanTokens;
    std::unordered_set<std::string> seen;

    for (const auto& tok : tokens) {
        std::string cleaned;
        cleaned.reserve(tok.size());
        for (char c : tok) {
            unsigned char uc = static_cast<unsigned char>(c);
            if (std::isalnum(uc) || c == '_') {
                cleaned.push_back(c);
            }
        }
        if (cleaned.size() >= 2 && seen.insert(cleaned).second) {
            cleanTokens.push_back(std::move(cleaned));
            if (cleanTokens.size() >= 8) break;
        }
    }

    std::string out;
    for (size_t i = 0; i < cleanTokens.size(); ++i) {
        if (i > 0) out += " OR ";
        out += cleanTokens[i] + "*";
    }
    return out;
}

std::vector<float> encodeConceptVector(const std::string& text, int dim) {
    int vecDim = std::max(1, dim);
    std::vector<float> vec(vecDim, 0.0f);
    projectTextIntoVector(vec, vecDim, text, 1.0f);
    l2Normalize(vec);
    return vec;
}

std::vector<float> encodeWeightedDocumentVector(
    const std::string& topicTitle,
    const std::string& subjectName,
    const std::string& formulaTags,
    const std::string& contentText,
    int dim
) {
    int vecDim = std::max(1, dim);
    std::vector<float> vec(vecDim, 0.0f);
    projectTextIntoVector(vec, vecDim, topicTitle, 3.6f);
    projectTextIntoVector(vec, vecDim, formulaTags, 2.3f);
    projectTextIntoVector(vec, vecDim, subjectName, 1.3f);
    projectTextIntoVector(vec, vecDim, contentText, 0.85f);
    l2Normalize(vec);
    return vec;
}

float computeTopicSpecificityScore(
    const std::string& query,
    const std::string& topicTitle,
    const std::string& subjectName,
    const std::string& formulaTags,
    const std::string& contentText,
    float denseSim,
    float ftsScore,
    float bm25Score
) {
    auto qTokens = tokenizeText(query);
    if (qTokens.empty()) return 0.0f;

    auto titleTokens = tokenizeText(topicTitle);
    auto tagTokens = tokenizeText(formulaTags + " " + subjectName);
    auto bodyTokens = tokenizeText(contentText);

    std::unordered_set<std::string> titleSet(titleTokens.begin(), titleTokens.end());
    std::unordered_set<std::string> tagSet(tagTokens.begin(), tagTokens.end());
    std::unordered_set<std::string> bodySet(bodyTokens.begin(), bodyTokens.end());
    const auto& generic = genericAcademicModifiers();

    int querySpecificCount = 0;
    int titleAllHits = 0;
    int titleSpecificHits = 0;
    int tagSpecificHits = 0;
    int bodySpecificHits = 0;

    std::unordered_set<std::string> uniqueQuery(qTokens.begin(), qTokens.end());
    for (const auto& qt : uniqueQuery) {
        bool isGen = (generic.find(qt) != generic.end());
        if (!isGen) querySpecificCount++;

        auto matchesSet = [&](const std::unordered_set<std::string>& setRef) -> bool {
            if (setRef.find(qt) != setRef.end()) return true;
            if (qt.size() >= 4) {
                for (const auto& candidate : setRef) {
                    if (candidate.size() >= 4 &&
                        (candidate.find(qt) == 0 || qt.find(candidate) == 0)) {
                        return true;
                    }
                }
            }
            return false;
        };

        if (matchesSet(titleSet)) {
            titleAllHits++;
            if (!isGen) titleSpecificHits++;
        } else if (matchesSet(tagSet)) {
            if (!isGen) tagSpecificHits++;
        } else if (matchesSet(bodySet)) {
            if (!isGen) bodySpecificHits++;
        }
    }

    auto qConcepts = detectActiveConcepts(qTokens);
    auto titleConcepts = detectActiveConcepts(titleTokens);
    auto tagConcepts = detectActiveConcepts(tagTokens);

    int sharedTitleConcepts = 0;
    int sharedTagConcepts = 0;
    for (const auto& qc : qConcepts) {
        if (titleConcepts.find(qc) != titleConcepts.end()) {
            sharedTitleConcepts++;
        } else if (tagConcepts.find(qc) != tagConcepts.end()) {
            sharedTagConcepts++;
        }
    }

    std::unordered_set<std::string> unionConcepts = qConcepts;
    unionConcepts.insert(titleConcepts.begin(), titleConcepts.end());
    float clusterJaccard = unionConcepts.empty()
        ? 0.0f
        : static_cast<float>(sharedTitleConcepts) / static_cast<float>(unionConcepts.size());

    // Exact phrase & bigram alignment between query and topic title
    std::string qLower = toLowerAscii(query);
    std::string tLower = toLowerAscii(topicTitle);
    float phraseBonus = 0.0f;
    if (tLower.size() >= 6 && qLower.find(tLower) != std::string::npos) {
        phraseBonus = 0.30f;
    } else if (qTokens.size() >= 2) {
        int bigramHits = 0;
        for (size_t i = 0; i + 1 < qTokens.size(); ++i) {
            std::string bg = qTokens[i] + " " + qTokens[i + 1];
            if (tLower.find(bg) != std::string::npos) bigramHits++;
        }
        if (bigramHits > 0) phraseBonus = 0.20f;
    }

    float titleHitRatio = static_cast<float>(titleAllHits) / static_cast<float>(std::max<size_t>(1, uniqueQuery.size()));
    float specificRecall = (querySpecificCount > 0)
        ? static_cast<float>(titleSpecificHits + 0.60f * tagSpecificHits + 0.22f * bodySpecificHits) / static_cast<float>(querySpecificCount)
        : titleHitRatio;
    specificRecall = std::clamp(specificRecall, 0.0f, 1.0f);

    float conceptCoverage = 0.0f;
    if (!qConcepts.empty()) {
        conceptCoverage = std::clamp(
            (static_cast<float>(sharedTitleConcepts) + 0.45f * static_cast<float>(sharedTagConcepts)) /
            static_cast<float>(qConcepts.size()),
            0.0f,
            1.0f
        );
    }

    // Orthogonal cluster conflict dampening (prevents cross-topic drift across all streams)
    float mismatchMultiplier = 1.0f;
    if (!qConcepts.empty() && sharedTitleConcepts == 0 && sharedTagConcepts == 0) {
        mismatchMultiplier = (titleSpecificHits == 0 && tagSpecificHits == 0) ? 0.18f : 0.40f;
    } else if (!qConcepts.empty() && !titleConcepts.empty() && sharedTitleConcepts == 0) {
        mismatchMultiplier = 0.42f;
    } else if (querySpecificCount > 0 && titleSpecificHits == 0 && tagSpecificHits == 0 && bodySpecificHits == 0) {
        mismatchMultiplier = 0.20f;
    }

    // Multi-feature calibrated regression score
    float linearScore = (0.34f * std::clamp(denseSim, 0.0f, 1.0f)) +
                        (0.12f * std::clamp(ftsScore, 0.0f, 1.0f)) +
                        (0.14f * std::clamp(bm25Score, 0.0f, 1.0f)) +
                        (0.18f * specificRecall) +
                        (0.12f * std::max(titleHitRatio, conceptCoverage)) +
                        (0.10f * clusterJaccard) +
                        phraseBonus;

    // Smooth logistic calibration centered around relevance boundary for crisp separation
    float centered = (linearScore * mismatchMultiplier) - 0.36f;
    float logistic = 1.0f / (1.0f + std::exp(-5.2f * centered));
    float calibrated = 0.55f * (linearScore * mismatchMultiplier) + 0.45f * logistic * mismatchMultiplier;
    return std::clamp(calibrated, 0.0f, 1.0f);
}

std::string composeFallbackTextbookResponse(
    const std::string& topicTitle,
    const std::string& subjectName,
    int chapterNumber,
    const std::string& contentText,
    const std::string& formulaTags,
    const std::string& workedExample,
    bool lowRamNotice
) {
    std::string out;
    out += "### " + subjectName + " • Ch. " + std::to_string(chapterNumber) + ": " + topicTitle + "\n\n";
    if (lowRamNotice) {
        out += "*Prepared using compact offline study mode.*\n\n";
    }
    out += trimAscii(contentText) + "\n\n";

    std::vector<std::string> formulas;
    size_t start = 0;
    while (start <= formulaTags.size()) {
        size_t comma = formulaTags.find(", ", start);
        std::string item = trimAscii(
            (comma == std::string::npos) ? formulaTags.substr(start) : formulaTags.substr(start, comma - start)
        );
        if (!item.empty()) {
            formulas.push_back(std::move(item));
        }
        if (comma == std::string::npos) break;
        start = comma + 2;
    }

    if (!formulas.empty()) {
        out += "### 1. Mathematical Formulation\n\n";
        out += "The core relationship for **" + topicTitle + "** is expressed as:\n\n";
        out += "$$\n" + cosmium::math::formatAsciiFormulaToLatex(formulas.front()) + "\n$$\n\n";
        for (size_t i = 1; i < formulas.size(); ++i) {
            out += "Related mathematical form for **" + topicTitle + "**:\n\n";
            out += "$$\n" + cosmium::math::formatAsciiFormulaToLatex(formulas[i]) + "\n$$\n\n";
        }
    }

    if (!trimAscii(workedExample).empty()) {
        out += "### 2. Worked Example\n\n";
        out += trimAscii(workedExample) + "\n\n";
    }

    return trimAscii(out);
}

float cosineSimilarity(const float* a, const float* b, int length) {
    if (!a || !b || length <= 0) return 0.0f;
    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < length; ++i) {
        double va = static_cast<double>(a[i]);
        double vb = static_cast<double>(b[i]);
        dot += va * vb;
        normA += va * va;
        normB += vb * vb;
    }
    double denom = std::sqrt(normA) * std::sqrt(normB);
    if (denom <= 1e-6) return 0.0f;
    return static_cast<float>(std::clamp(dot / denom, 0.0, 1.0));
}

bool evaluateQuizAnswer(
    const std::string& studentRaw,
    const std::string& canonicalRaw,
    const std::string& keywordsPipe
) {
    std::string studentNorm = normalizeSpeechAndMath(studentRaw);
    if (studentNorm.empty()) return false;
    std::string studentCompact = stripSpacesAndParens(studentNorm);

    std::vector<std::string> keywords;
    size_t start = 0;
    while (start <= keywordsPipe.size()) {
        size_t pos = keywordsPipe.find('|', start);
        std::string item = (pos == std::string::npos)
            ? keywordsPipe.substr(start)
            : keywordsPipe.substr(start, pos - start);
        std::string kwNorm = normalizeSpeechAndMath(item);
        if (!kwNorm.empty()) keywords.push_back(std::move(kwNorm));
        if (pos == std::string::npos) break;
        start = pos + 1;
    }

    for (const auto& kw : keywords) {
        if (studentNorm.find(kw) != std::string::npos) return true;
        std::string kwCompact = stripSpacesAndParens(kw);
        if (kwCompact.size() >= 2 && studentCompact.find(kwCompact) != std::string::npos) {
            return true;
        }
    }

    if (!canonicalRaw.empty()) {
        std::string canonicalNorm = normalizeSpeechAndMath(canonicalRaw);
        std::string canonicalCompact = stripSpacesAndParens(canonicalNorm);
        if (!canonicalCompact.empty() && studentCompact.find(canonicalCompact) != std::string::npos) {
            return true;
        }
    }

    return keywords.empty() && studentNorm.size() >= 4;
}

} // namespace cosmium::rag
