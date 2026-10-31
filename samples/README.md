# Sample Labels

Synthetic label images for trying the app. The brands are made up.

`batch.csv` holds the application data for every image, so you can upload it together with the images in **Batch Upload** mode. To test **Single Label** mode instead, copy one row's values into the form.

| File | What it tests | Expected result |
|---|---|---|
| `01-old-tom-bourbon.png` | Clean label where every field matches | All fields PASS |
| `02-stones-throw-vodka.png` | Case and apostrophe differences (`STONE’S THROW` vs `Stone's Throw`), plus unit conversion (`1 L` vs `1000 mL`) | All fields PASS |
| `03-abv-mismatch.png` | Label says 40%, application says 45% | Alcohol Content FAIL |
| `04-warning-title-case.png` | Warning title written as `Government Warning:` | Government Warning Text FAIL |
| `05-imperfect-photo.jpg` | Tilted, blurry, noisy photo | Most fields NEEDS_REVIEW (OCR cannot read them reliably) |
| `06-chateau-red-wine.png` | Serif font, colored ink, `ALC./VOL.` format | All fields PASS |
| `07-missing-image.png` | Row in the CSV with no matching image | Batch row shows an error; the rest of the batch continues |

"Warning Formatting" is always NEEDS_REVIEW, because bold styling and type size cannot be verified from OCR text. As a result, the overall status is never PASS.
