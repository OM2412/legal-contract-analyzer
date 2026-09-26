package com.omjadon.contractanalyzer.sample;

import com.omjadon.contractanalyzer.model.SourceDocument;

/**
 * Short synthetic excerpts used only for development.
 */
public final class SampleDocuments {

    private SampleDocuments() {
    }

    public static SourceDocument agreement() {
        return new SourceDocument(
                "agreement-001",
                "1",
                """
                SYNTHETIC SERVICE AGREEMENT EXCERPT

                Client: Cedar Labs
                Provider: Orbit Software

                1. Services
                Provider will develop a booking interface for Client.

                2. Payment
                Client shall pay Provider the project fee within
                30 calendar days after receipt of the invoice.

                3. Payment precedence
                If this Agreement and the SOW differ on payment for the project fee, the SOW payment term prevails.
                """
        );
    }

    public static SourceDocument statementOfWork() {
        return new SourceDocument(
                "sow-001",
                "1",
                """
                SYNTHETIC STATEMENT OF WORK EXCERPT

                Client: Cedar Labs
                Provider: Orbit Software

                1. Deliverable
                Provider will deliver the booking interface.

                2. Payment
                Client shall pay Provider the project fee within
                60 calendar days after receipt of the invoice.
                """
        );
    }
}