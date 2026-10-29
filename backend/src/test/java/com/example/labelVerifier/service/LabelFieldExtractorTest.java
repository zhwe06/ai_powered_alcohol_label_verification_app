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
                NET CONTENTS 750 mL
                GOVERNMENT WARNING:
                (1) According to the Surgeon General,
                women should not drink alcoholic beverages
                during pregnancy because of the risk
                of birth defects.
                """;

        DetectedLabelFields fields =
                extractor.extract(
                    ocrText, "Old Tom Distillery"
                );

        assertEquals(
            "OLD TOM DISTILLERY",
            fields.brandName()
        );

        assertEquals(
            "45% ALC./VOL.",
            fields.alcoholContent()
        );
        System.out.println(fields.netContents());
        assertEquals(
            "750 mL",
            fields.netContents()
        );

        assertNotNull(fields.governmentWarning());
    }

    @Test
    void shouldExtractAlcoholByVolumeFormat() {

        String ocrText = """
            Alcohol Facts
            Serving Size 3.4 fl oz (100 mL)
            Alcohol by volume 17%
            Calories 100
            Carbohydrate 2g
            """;

        DetectedLabelFields result =
            extractor.extract(
                ocrText, "Old Tom Distillery"
            );

        assertNull(result.brandName());

        assertEquals(
            "Alcohol by volume 17%",
            result.alcoholContent()
        );
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
            extractor.extract(ocrText, "Test Brand");

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
            extractor.extract(ocrText, "Test Brand");

        assertEquals("750 mL", result.netContents());
    }

    @Test
    void shouldReturnNullForMissingFields() {

        DetectedLabelFields fields =
                extractor.extract(
                    "KENTUCKY BOURBON",
                    "Old Tom Distillery"
                );

        assertNull(fields.brandName());
        assertNull(fields.alcoholContent());
        assertNull(fields.netContents());
        assertNull(fields.governmentWarning());
    }

    @Test
    void shouldHandleEmptyOcrText() {

        DetectedLabelFields fields =
                extractor.extract(
                    "",
                    "Old Tom Distillery"
                );

        assertNull(fields.brandName());
        assertNull(fields.alcoholContent());
    }

    @Test
    void shouldNotGuessWrongBrand() {

        DetectedLabelFields fields =
                extractor.extract(
                    "ANOTHER DISTILLERY\n40% ABV",
                    "Old Tom Distillery"
                );

        assertNull(fields.brandName());

        assertEquals(
            "40% ABV",
            fields.alcoholContent()
        );
    }
}
