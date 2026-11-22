# Payment workflow pilot — source-integrity amendment

The original `PAIR-001` is a **self-written, development-used workflow pilot**, not an independently sampled real contract or an untouched held-out case. Its directory name (`heldout`) must not be interpreted as proof of independent evaluation.

On 2026-11-23 (Asia/Kolkata), the product-label validator exposed a mismatch between the stored manifest hashes and the committed source bytes. The payment-obligation quotes were still present **exactly at the annotated code-point offsets** in both documents. The manifest `sha256` fields have therefore been amended to describe the actual committed UTF-8 files:

| Document | Previously recorded SHA-256 | Verified current SHA-256 |
| --- | --- | --- |
| `PAIR-001-agreement.txt` | `285ca7acbb8e791c4b0dd5d5bdc74c03ddb056ac89e36e9be572c555178a6f73` | `3caea13b027761de4b235cfdc48366c3207277988166095a22613a5e50b3eee9` |
| `PAIR-001-sow.txt` | `c971cc96352c3f390df0c746cb83bf6cf96a85983c53ad3028b5208cca5b692a` | `461e4a1efec29dc953ecd0550f2d64188f5d460d9357ae6a4789bd500984878a` |

This is an integrity repair, **not** a relabeling of obligations or a retroactive improvement in accuracy. Earlier saved prediction artifacts contain the old `documentVersion` hashes and must not be compared with this amended manifest. CI now obtains fresh `/api/review` predictions for this specific pilot and scores those against the validated source/labels.

The resulting matched/missed count demonstrates that the scoring pipeline works. It must **not** be presented as real-world recall, legal accuracy, independent AI performance, or representative production performance.

To establish real-world quality, collect new contract pairs with reuse permission, have independent annotators freeze labels and source checksums **before** exposing documents to the system, and score them exactly once without adapting rules to the test set. Keep model-backed suggestions, deterministic payment extraction and legal enforceability separate in reporting.
