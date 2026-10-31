package com.example.labelverifier.service;

import com.example.labelverifier.dto.DetectedLabelFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelFieldExtractorTest {

    private final LabelFieldExtractor extractor =
            new LabelFieldExtractor();

    @Test
    void shouldExtractLabelFields() {

        String ocrText = """
                OLD TOM DISTILLERY
                KENTUCKY BOURBON
                45% ALC./VOL.
                750 mL
                GOVERNMENT WARNING:
                (1) According to the Surgeon General,
                women should not drink alcoholic beverages
                during pregnancy because of the risk
                of birth defects.
                """;

        DetectedLabelFields fields =
                extractor.extract(
                        ocrText, "Old Tom Distillery", null
                );

        assertEquals(
                "OLD TOM DISTILLERY",
                fields.brandName()
        );

        assertEquals(
                "45% ALC./VOL.",
                fields.alcoholContent()
        );
        assertEquals(
                "750 mL",
                fields.netContents()
        );

        assertNotNull(fields.governmentWarning());
    }

    @Test
    void shouldPreferExactBrandOverEarlierCloseMatch() {
        String ocrText = """
                OLD T0M DISTILLERY
                OLD TOM DISTILLERY
                45% ABV
                """;

        DetectedLabelFields result =
                extractor.extract(ocrText, "Old Tom Distillery", null);

        assertEquals(
                "OLD TOM DISTILLERY",
                result.brandName()
        );
    }

    @Test
    void shouldExtractAlcoholByVolumeFormat() {

        String ocrText = """
                null
                Serving Size 3.4 fl oz (100 mL)
                Alcohol by volume 17%
                Calories 100
                Carbohydrate 2g
                """;

        DetectedLabelFields result =
                extractor.extract(
                        ocrText, "Old Tom Distillery", null
                );

        assertEquals(
                "Alcohol by volume 17%",
                result.alcoholContent()
        );
    }

    @Test
    void shouldMatchBrandWithCurlyApostrophe() {

        String ocrText = """
                STONE\u2019S THROW
                Alcohol Facts
                Serving Size 3.4 fl oz (100 mL)
                Alcohol by volume 17%
                Calories 100
                Carbohydrate 2g
                """;

        DetectedLabelFields result =
                extractor.extract(
                        ocrText, "Stone's Throw", null
                );

        assertNotNull(result.brandName());

        assertEquals(
                "STONE\u2019S THROW",
                result.brandName()
        );
    }

    @Test
    void shouldReturnCloseBrandMatch() {

        String ocrText = """
                OLD T0M DISTILLERY
                45% ABV
                Alcohol Facts
                Serving Size 3.4 fl oz (100 mL)
                Calories 100
                Carbohydrate 2g
                """;

        DetectedLabelFields result =
                extractor.extract(
                        ocrText, "Old Tom Distillery", null
                );

        assertNotNull(result.brandName());

        assertEquals(
                "OLD T0M DISTILLERY",
                result.brandName()
        );
    }

    @Test
    void shouldFindBrandInsideLongerLine() {

        String ocrText = """
                OLD TOM DISTILLERY EST. 1891
                Alcohol Facts
                Serving Size 3.4 fl oz (100 mL)
                Alcohol by volume 17%
                Calories 100
                Carbohydrate 2g
                """;

        DetectedLabelFields result =
                extractor.extract(
                        ocrText, "Old Tom Distillery", null
                );

        assertNotNull(result.brandName());

        assertEquals(
                "OLD TOM DISTILLERY EST. 1891",
                result.brandName()
        );
    }

    @Test
    void shouldNotMatchBrandInsideAnotherWord() {

        String ocrText = """
                SPACE WHISKEY
                Alcohol Facts
                Serving Size 3.4 fl oz (100 mL)
                Alcohol by volume 17%
                Calories 100
                Carbohydrate 2g
                """;

        DetectedLabelFields result =
                extractor.extract(
                        ocrText, "Ace", null
                );

        assertNull(result.brandName());
    }

    @Test
    void shouldNotConfuseServingSizeWithNetContents() {

        String ocrText = """
                Alcohol Facts
                Serving Size 3.4 fl.oz (100 mL)
                Alcohol by volume 17%
                Calories 100
                """;

        DetectedLabelFields result =
                extractor.extract(ocrText, "Test Brand", null);

        assertNull(result.netContents());
    }

    @Test
    void shouldExtractExplicitNetContents() {

        String ocrText = """
                TEST BRAND
                45% ABV
                NET CONTENTS 750 mL
                """;

        DetectedLabelFields result =
                extractor.extract(ocrText, "Test Brand", null);

        assertEquals("750 mL", result.netContents());
    }

    @Test
    void shouldReturnNullForMissingFields() {

        DetectedLabelFields fields =
                extractor.extract(
                        "KENTUCKY BOURBON",
                        "Old Tom Distillery", null
                );

        assertNull(fields.alcoholContent());
        assertNull(fields.netContents());
        assertNull(fields.governmentWarning());
    }

    @Test
    void shouldHandleEmptyOcrText() {

        DetectedLabelFields fields =
                extractor.extract(
                        "",
                        "Old Tom Distillery", null
                );

        assertNull(fields.brandName());
        assertNull(fields.alcoholContent());
    }

    @Test
    void shouldNotGuessWrongBrand() {

        DetectedLabelFields fields =
                extractor.extract(
                        "ANOTHER DISTILLERY\n40% ABV",
                        "Old Tom Distillery", null
                );

        assertNull(fields.brandName());

        assertEquals(
                "40% ABV",
                fields.alcoholContent()
        );
    }

    @Test
    void shouldExtractClassType() {
        String ocrText = """
            OLD TOM DISTILLERY
            Kentucky Straight Bourbon Whiskey
            45% ABV
            """;

        DetectedLabelFields result = extractor.extract(
                ocrText, "Old Tom Distillery", "Kentucky Straight Bourbon Whiskey");

        assertEquals("Kentucky Straight Bourbon Whiskey", result.classType());
    }

    @Test
    void shouldReturnNullClassTypeWhenNotProvided() {
        String ocrText = """
            OLD TOM DISTILLERY
            Kentucky Straight Bourbon Whiskey
            45% ABV
            """;

        DetectedLabelFields result = extractor.extract(
                ocrText, "Old Tom Distillery", null);

        assertNull(result.classType());
    }
}
