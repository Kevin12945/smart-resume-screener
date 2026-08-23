# Smart Resume Screener

Parses resumes (PDF/text), extracts structured candidate data using an LLM, and
computes a semantic 1–10 fit score against a job description, with a written
justification — so recruiters can see a ranked, explainable shortlist.

## Architecture

```
Client (curl / Postman)
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│                     Spring Boot API                        │
│                                                              │
│  JobDescriptionController   ResumeController   MatchController │
│         │                        │                    │     │
│         ▼                        ▼                    ▼     │
│  JobDescriptionRepository   PdfTextExtractionService  MatchingService │
│                                   │                    │     │
│                                   ▼                    │     │
│                          ResumeExtractionService ◄─────┘     │
│                                   │                           │
│                                   ▼                           │
│                              LlmService                       │
│                          (Gemini API)                          │
└───────────────────────────────────────────────────────────┘
        │
        ▼
   H2 database (file-based, ./data/screener.mv.db)
```

**Flow:**
1. `POST /api/resumes/upload` — a PDF/txt resume is uploaded. `PdfTextExtractionService`
   (Apache PDFBox) pulls raw text out of the file.
2. `ResumeExtractionService` sends that raw text to Gemini with a strict-JSON
   extraction prompt, producing structured `skills`, `experience`, and `education`.
   The resume + extracted fields are persisted.
3. `POST /api/job-descriptions` stores a job description.
4. `POST /api/match/{resumeId}/{jdId}` sends the resume text and JD to Gemini with
   a scoring prompt. The model returns a `score` (1–10), a `justification`,
   and matched/missing skill lists — all persisted as a `MatchResult`.
5. `GET /api/match/shortlist?jobDescriptionId=..&minScore=..` returns candidates
   for that JD, ranked by score, with their justification.

## Tech Stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 (file-based) — zero external DB setup required to run/evaluate
- Apache PDFBox — PDF text extraction
- Google Gemini API (free tier, no credit card) — semantic extraction & scoring
- Lombok — boilerplate reduction

## Project Structure

```
src/main/java/com/screener/
├── SmartResumeScreenerApplication.java
├── config/AppConfig.java              # RestTemplate bean
├── controller/                        # REST endpoints
├── service/
│   ├── PdfTextExtractionService.java  # PDF/text -> raw text
│   ├── LlmService.java                # Gemini API client
│   ├── ResumeExtractionService.java   # raw text -> structured JSON via LLM
│   └── MatchingService.java           # resume + JD -> score via LLM
├── entity/                            # JPA entities
├── repository/                        # Spring Data repositories
├── dto/                                # request/response payloads
└── exception/                         # centralized error handling
```

## Setup & Run

**Prerequisites:** Java 17+, Maven 3.9+, a free Gemini API key.

1. Get a free key (no credit card required) at https://aistudio.google.com/apikey
2. Set it as an environment variable and run:

```bash
export GEMINI_API_KEY=AIza...
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. The model used is configurable in
`application.properties` via `llm.gemini.model` (defaults to `gemini-2.5-flash`,
which is on Google's free tier).

## API Usage

**1. Create a job description**
```bash
curl -X POST http://localhost:8080/api/job-descriptions \
  -H "Content-Type: application/json" \
  -d '{"title": "Backend Engineer", "description": "Looking for a Java/Spring Boot engineer with REST API and SQL experience..."}'
```

**2. Upload a resume**
```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -F "file=@/path/to/resume.pdf"
```

**3. Run the match**
```bash
curl -X POST http://localhost:8080/api/match/1/1
```

**4. Get the shortlist**
```bash
curl "http://localhost:8080/api/match/shortlist?jobDescriptionId=1&minScore=6"
```

## LLM Prompts

### Extraction prompt (`ResumeExtractionService`)
Instructs the model to act as a resume-parsing engine and return **only** a JSON
object with `candidateName`, `skills`, `experience`, and `education` — with an
explicit rule to never omit keys and never invent unstated information, so
downstream parsing is reliable.

### Scoring prompt (`MatchingService`)
Based on the assignment's suggested prompt, extended to force structured output:

> *"You are an expert technical recruiter. Compare the following resume with
> the given job description and rate the candidate's fit on a scale of 1–10
> ... with a clear justification ... Be honest and critical — do not inflate
> scores out of politeness."*

It returns `score`, `justification`, `matchedSkills`, and `missingSkills` as
JSON so the API can serve ranked, explainable results without any further
LLM calls per shortlist request.

## Notes

- No `.env`, `node_modules`, or build artifacts are committed — see `.gitignore`.
- Database is file-based H2 (`./data/screener.mv.db`), created automatically on
  first run; no external DB install is needed to evaluate this project.
- Swapping providers: `LlmService` is the single integration point — replacing
  the Gemini call with an Anthropic or OpenAI call only requires changes in that one class.
