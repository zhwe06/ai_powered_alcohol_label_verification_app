# Alcohol Label Verification

A decision-support tool that helps TTB (Alcohol and Tobacco Tax and Trade Bureau) reviewers compare label images against
the application data they were submitted with. It flags mismatches and
uncertain cases for human review — it does **not** make the final approval
decision.

- **Live demo:** https://label-verifier.mangorock-0ee6f030.eastus.azurecontainerapps.io
- **Source:** https://github.com/zhwe06/ai_powered_alcohol_label_verification_app

## Try It in 1 Minute

1. Open the live demo and click **Batch Upload**.
2. Choose `samples/batch.csv`, then select the six images in `samples/` → **Verify All**.
3. The expected result for each sample is documented in [`samples/README.md`](samples/README.md).

To test a single label instead, switch to **Single Label** and copy one row's
values from `samples/batch.csv` into the form.

## What It Checks

| Field | How it is checked | Possible results |
|---|---|---|
| Brand Name | Normalized (case, spacing, curly→straight quotes), then exact whole-phrase match | PASS / NEEDS_REVIEW (never FAIL) |
| Class / Type | Same normalized phrase matching as Brand Name | PASS / NEEDS_REVIEW (never FAIL) |
| Alcohol Content | Parsed to a number and compared exactly (`45% ABV` = `45% Alc./Vol.`) | PASS / FAIL / NEEDS_REVIEW |
| Net Contents | Converted to mL and compared exactly (`1 L` = `1000 mL`) | PASS / FAIL / NEEDS_REVIEW |
| Government Warning Text | Must match the 27 CFR 16.21 wording exactly; the title `GOVERNMENT WARNING:` must be all caps | PASS / FAIL / NEEDS_REVIEW |
| Warning Formatting | Bold, type size, contrast and placement | Always NEEDS_REVIEW |

Brand name and class/type use two passes: an exact phrase match gives PASS; a
fuzzy (Levenshtein) similarity ≥ 0.8 is still only NEEDS_REVIEW (and reports
the closest matching line); anything weaker is NEEDS_REVIEW. These fields are
never auto-FAILed, because an OCR misread is not proof the label is wrong.

The three statuses:

- **PASS** — the detected text matches the application data.
- **FAIL** — a hard mismatch (e.g. wrong ABV, title-cased warning).
- **NEEDS_REVIEW** — the system cannot decide and hands it to a human.

The overall status follows a strict rule: any FAIL ⇒ FAIL; otherwise any
NEEDS_REVIEW ⇒ NEEDS_REVIEW; otherwise PASS.

> ⚠️ **Deliberate design note:** because Warning Formatting is always
> NEEDS_REVIEW, the overall status is **never PASS**. This is intentional —
> formatting must always be seen by a human.

## Architecture

```
Browser (React)
   │  POST /api/v1/verifications   (multipart: image + application JSON)
   ▼
Spring Boot
   ├─ Tess4J (Tesseract OCR, runs locally in the container)
   ├─ LabelFieldExtractor          ← finds the submitted values in the OCR text
   ├─ Validators                   ← one per field
   ▼
JSON { status, checks: [...] }
```

OCR runs inside the container and calls **no external API**.

Batch mode: the browser reads the CSV and calls this same endpoint once per
image — the backend has no separate batch endpoint.

## Deployment

```
git push ─▶ GitHub Actions ─▶ docker build (Dockerfile) ─▶ push to GHCR
                                                              │
                          Azure Container Apps ◀──────────────┘
                          (pulls and runs the image)
```

- Sized at **1 vCPU / 2 GiB**, **min replicas 1** (so there is no cold start).
- The `Dockerfile` is a multi-stage build: **Node** builds the frontend →
  **Maven** builds the jar → a **JRE + Tesseract** runtime image.

**Why GHCR instead of ACR:** the Azure free-trial subscription does not allow
ACR Tasks (cloud builds), so GitHub Actions builds the image and pushes it to
GitHub Container Registry. In a real agency environment this would switch to
the agency's own Azure Container Registry.

## Running Locally

**Prerequisites:** Java 21, Node 24. The Tesseract language data is already in
the repo.

```bash
# Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# Frontend dev server (http://localhost:5173, proxies /api to 8080)
cd frontend
npm install
npm run dev

# Tests
cd backend
./mvnw test

# Same image as production
docker build -t label-verifier .
docker run -p 8080:8080 label-verifier
```

Environment variables: `PORT` (default `8080`) and `TESSDATA_PATH` (already
set inside the Docker image).

## Approach and Key Decisions

1. **Local OCR (Tesseract) instead of a cloud AI model.**
   Stakeholder interviews noted a firewall that blocks outbound ML APIs. Local OCR keeps
   data inside the system boundary and produces reproducible, explainable
   output. *Cost:* Tesseract reads blurry photos less reliably than cloud vision
   models. *Flexibility:* OCR is hidden behind the `OcrService` interface — if
   the agency later allows Azure AI Vision over a private endpoint, it can be
   added as a new implementation without touching any other code.

2. **Rule-based checks plus human review, not a model that decides.**
   Regulatory review requires every judgment to be explainable. When unsure, the
   system returns NEEDS_REVIEW rather than risking a false PASS.

3. **Search for the *expected* values instead of blindly extracting text.**
   The application data is already known, so matching it against the OCR text is
   far more reliable than guessing which line is the brand name.

4. **Brand name and class/type are never automatically FAILed.**
   Differences like `STONE'S THROW` vs `Stone's Throw` need human judgment, and
   an OCR misread (`T0M` vs `TOM`) is not evidence the label is wrong.

5. **Batch upload is processed one label at a time in the frontend.**
   The backend stays unchanged; OCR is CPU-bound, so parallel requests would not
   finish sooner and could exhaust memory. *Cost:* closing the tab aborts the
   batch — a production version should use a backend queue.

## Assumptions

- The submitted application data (form or CSV) is the correct source of truth.
- Labels are in English; images are JPG/PNG up to 10 MB.
- Each image contains a single label side.
- The required government warning text follows 27 CFR 16.21 and lives in
  `backend/src/main/resources/regulations/government-warning.txt`.
- Alcohol content requires exact numeric equality; TTB's allowed tolerance is
  not implemented.

## Performance

| Environment | Per label | 300 labels |
|---|---|---|
| Local laptop | 0.2–0.5 s | ~1–3 min |
| **Azure Container Apps (1 vCPU)** | **2–3 s** | **~10–15 min** |
| Render free tier (0.1 vCPU) | ~15 s | ~75 min |

The 5-second target is met on Azure. OCR speed is CPU-bound, so moving from
Render's 0.1 vCPU to a 1 vCPU container solved it — no need to change the OCR
engine. (Numbers come from the synthetic labels in `samples/`; real photos are
may be slower.)

## Known Limitations

- Tilted, blurry photos degrade most fields to NEEDS_REVIEW (see `samples/05-imperfect-photo.jpg`).
  The system degrades safely to manual review instead of guessing wrong.
- Bold styling and type size cannot be verified from OCR text.
- Bottler and Country of Origin are not implemented.
- Brand similarity is line-based: if the brand shares a line with other text,
  the score is dragged down.
- Batch processing runs in the browser; closing the page stops it.
- No login, no audit log, and results are not persisted.
- Azure deployments currently require a manual `az containerapp update`.


## Future Work

- Image preprocessing before OCR: grayscale, denoise, deskew.
- Azure AI Vision as a second `OcrService` implementation (via private endpoint).
- A backend job queue (e.g. Azure Service Bus) so batches don't depend on the browser.
- Microsoft Entra ID login, audit logging, and result persistence.
- Use OCR position/font information to check bold and type size.
- Add Bottler and Country of Origin (the generic matching approach already exists).
- Deploy automatically after the GitHub Actions build (remove the manual
  `az containerapp update` step).

## Development Notes

- Claude Code was used for tutoring and code review.
- The label images in `samples/` are synthetic, generated by a script; the
  brands are fictional.