# Local account and saved-review API validation

Verified on 14 October 2026 against the local backend on port 8081.

Run ID: 15223830-685b-4dd9-a107-d4f88b0c98c8
Outcome: 38 checks passed, zero failures, completed successfully.

## Verified behaviors

- Anonymous profile and review access rejected.
- Registration without CSRF rejected with 401.
- Valid registration succeeds; duplicate email rejected.
- Incorrect login rejected; valid login establishes account identity.
- Separate clients maintain different account identities.
- Authenticated uploads without or with invalid CSRF rejected with 403.
- Valid upload preserves source versions and expected clause counts.
- Backend-computed review can be saved and read by its owner.
- Owner list includes the saved review.
- Another account receives 404 for the owner's review.
- Another account's list excludes the owner's review.
- Unknown review returns 404.
- Logout without CSRF rejected.
- Valid logout succeeds; subsequent protected access rejected.

## Boundaries

Anonymous registration rejection accepts either 401 or 403;
authenticated CSRF checks strictly require 403.
The report records the actual status for each request.

These are live integration checks for the tested local configuration.
They do not establish penetration-test coverage, production-cookie
security, rate limiting, password recovery or concurrency safety.

Two generated test accounts and a synthetic saved review remain in
the local database. Passwords, cookies and tokens are not stored in
the result report.

The earlier incomplete run is retained locally as development evidence.