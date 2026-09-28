package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEvaluation003Test {

    @Test
    void extractsLabeledFactsFromUnseenWordingCombination()
            throws Exception {
        String clause = Files.readString(
                Path.of(
                        "testdata", "evaluation",
                        "EVAL-003-agreement.txt"
                ),
                StandardCharsets.UTF_8
        ).trim();

        SourceDocument document = new SourceDocument(
                "evaluation-003", "1", clause
        );

        List<PaymentTerm> terms =
                PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());

        PaymentTerm term = terms.get(0);
        assertEquals("Customer", term.payer());
        assertEquals("Provider", term.payee());
        assertEquals("implementation_fee", term.scope());
        assertEquals(15, term.days());
        assertEquals(PaymentTerm.DayUnit.BUSINESS_DAYS, term.dayUnit());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals(clause, term.evidence().quote());
        EvidenceValidator.validate(document, term.evidence());

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        assertEquals(
                PaymentPolicyEvaluator.Status.UNABLE_TO_ASSESS,
                PaymentPolicyEvaluator.evaluate(document, term, policy)
                        .status()
        );
    }
}