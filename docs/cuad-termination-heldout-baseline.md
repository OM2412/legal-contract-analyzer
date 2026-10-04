CUAD termination held-out candidate retrieval audit
Run date: 4 October 2026
Source: CUAD test.json (released test split)
Scanner: TerminationSignalFinder
Source commit: `61edea3`
Unit: contract title with a Termination For Convenience question
Measure	Result
Evaluated contracts / paragraphs	102 / 102
Target question instances	           102
Gold answer spans	                    41
Gold-positive contracts              	29
Scanner-positive contracts          	55
Both positive	                        25
Scanner only	                        30
Gold only	                             4
Both negative                        	43
Both positive with at least one evidence overlap	20
Both positive without evidence overlap	5


Candidate flag precision: 25 / 55 = 0.455
Candidate flag recall: 25 / 29 = 0.862
Candidate flag F1: 50 / 84 = 0.595
The four contract-level cells sum to 102. Among 25 contracts with both a gold answer and a scanner finding, 20 had at least one overlapping predicted and annotated span. Overlap is a binary contract-level check, not a span-level precision or recall estimate.
Interpretation
The scanner detects generic termination wording, while CUAD's Termination For Convenience label concerns termination without cause. Thus the 30 scanner-only contracts cannot automatically be called legal false positives, and the metrics measure overlap with that narrower label rather than correct legal classification. An evidence overlap alone does not establish that the cue has the correct legal meaning. This is also not an Agreement-versus-SOW product accuracy estimate.
This test run is reported separately from the earlier training-split audit used to develop the scanner. Do not adjust the scanner using these test examples and continue to describe this same split as untouched held-out evaluation. The run completed with BUILD SUCCESS.
