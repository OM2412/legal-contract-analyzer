package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNamedInstallmentExtractionTest {

    private static final String CLAUSE =
            "Customer must pay Atlas Systems 35 percent of the "
                    + "migration fee within 12 business days "
                    + "after the invoice date.";

    @Test
    void extractsExplicitInstallmentWithExactFieldsAndEvidence() {
        SourceDocument document =
                new SourceDocument("sow", "1", CLAUSE);

        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());

        var term = terms.get(0);

        assertEquals(12, term.days());
        assertEquals(
                PaymentTerm.DayUnit.BUSINESS_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_DATE,
                term.trigger()
        );
        assertEquals("Customer", term.payer());
        assertEquals("Atlas Systems", term.payee());
        assertEquals("35 percent of the migration fee", term.scope());
        assertEquals(CLAUSE, term.evidence().quote());

        EvidenceValidator.validate(document, term.evidence());

        var policy = new PaymentPolicy(
                "P-DEMO-30", "1.0", 30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        assertEquals(
                PaymentPolicyEvaluator.Status.UNABLE_TO_ASSESS,
                PaymentPolicyEvaluator.evaluate(
                        document, term, policy
                ).status()
        );
    }

    @Test
    void negationAndInvalidPercentageAreNotExtracted() {
        for (String text : new String[] {
                CLAUSE.replace("must pay", "must not pay"),
                CLAUSE.replace("35 percent", "101 percent")
        }) {
            assertTrue(PaymentTermExtractor.extractAll(
                    new SourceDocument("negative", "1", text)
            ).isEmpty());
        }
    }

    @Test
    void percentSymbolKeepsTheExactQuote() {
        String text = CLAUSE.replace("35 percent", "35%");

        SourceDocument document =
                new SourceDocument("symbol", "1", text);

        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());
        assertEquals(
                "35 percent of the migration fee",
                terms.get(0).scope()
        );
        assertEquals(text, terms.get(0).evidence().quote());
        EvidenceValidator.validate(document, terms.get(0).evidence());
    }

    @Test
    void nonBindingExampleIsNotExtracted() {
        SourceDocument document = new SourceDocument(
                "example", "1",
                "This illustrative example is not binding: \""
                        + CLAUSE + "\""
        );

        assertTrue(
                PaymentTermExtractor.extractAll(document).isEmpty()
        );
    }
}