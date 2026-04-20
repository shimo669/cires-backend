# Testing Strategy (Phase 10: Testing & Debugging)

## Scope
- API and service behavior for report resolution confirmation and rating.
- Error handling consistency for forbidden, invalid, and not-found operations.
- Lightweight performance improvement in admin report mapping (`feedback` batch loading).

## Test Layers
1. **Unit tests (service layer)**
   - `ReportServiceConfirmResolutionTest`
   - `InteractionServiceTest`
2. **Smoke test (application context)**
   - `CiresBackendApplicationTests`

## Key Scenarios
- Leader marks issue solved -> report status becomes `PENDING_REPORTER_CONFIRMATION`.
- Reporter approves with rating (1-5) -> report becomes `RESOLVED`, feedback persisted.
- Reporter rejects -> report becomes `REOPENED`, SLA timer restarted.
- Non-reporter trying to submit feedback -> forbidden.
- Invalid rating -> bad request semantics.
- History API safely handles system-generated actions (`actedBy = SYSTEM`).

## Error Handling Rules
- `ResourceNotFoundException` -> 404
- `ForbiddenActionException` -> 403
- `InvalidRequestException` -> 400
- Unhandled exceptions -> 500

## Commands Used
```powershell
Set-Location 'C:\Users\USER\Documents\cires-backend'
.\mvnw.cmd test
```

## Sample Results (from current run)
- Total tests run: 8
- Result: **PASS**
- Notes:
  - `InteractionServiceTest`: 4 tests passed.
  - `ReportServiceConfirmResolutionTest`: 3 tests passed.
  - `CiresBackendApplicationTests`: 1 smoke/context test passed.

## Debugging Checklist
- Confirm JWT username matches report reporter on feedback/confirm endpoints.
- Verify status transition path:
  - `PENDING`/`ESCALATED` -> `PENDING_REPORTER_CONFIRMATION` -> (`RESOLVED` or `REOPENED`)
- Validate DB rows in `feedback` include:
  - `approved`, `rating`, `comment`, `confirmed_at`
- Verify admin dashboard endpoint `/api/admin/reports` returns confirmation fields.

