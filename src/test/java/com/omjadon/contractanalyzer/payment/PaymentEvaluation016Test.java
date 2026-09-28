package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEvaluation016Test {

    @Test
    void disclaimerAfterQuotedExampleLeavesOnlyOperativeClause() {
        String text = "Example wording: \"Client shall pay Provider "
                + "the project fee within 30 calendar days after "
                + "receipt of the invoice.\" This example creates "
                + "no payment obligation. Client shall pay Provider "
                + "the project fee within 45 calendar days after "
                + "receipt of the invoice.";

        SourceDocument document = new SourceDocument(
                "agreement", "1", text
        );

        List<PaymentTerm> terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());
        assertEquals(45, terms.get(0).days());
        assertEquals(
                "Client shall pay Provider the project fee within "
                        + "45 calendar days after receipt of the invoice.",
                terms.get(0).evidence().quote()
        );
    }
}