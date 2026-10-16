# Test Case: Visit Booked for Known Pet

## Overview

**ID:** TC-001  
**Goal:** A clinic user finds an existing owner, picks one of the pets already on record and books a visit for it — verifying the visit is added to that pet's history.  
**Priority:** High  
**Status:** Approved  
**Process:** [BP-005 Book Visit](../processes/BP-005-book-visit.bpmn) — Owner requests a visit → UC-004 Find Owners by Last Name → Owner on record? yes → UC-005 View Owner Details → Pet on record? yes → UC-009 Book Visit for Pet → Visit booked

## Roles

- Clinic User (finds the owner and books the visit)

## Preconditions

- The owner "Maria Escobito", 345 Maple St., Madison, 6085557683, exists and is the only owner whose last name starts with "Escobito" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner on record? yes" and makes the search land directly on her details.
- Maria Escobito owns the pet "Mulligan" (dog, born 1997-02-24) with no visits (Flyway test data `V2__seed_reference_data.sql`) — this forces "Pet on record? yes".

## Flow

| Step | Name                  | Description                                                                                                                  | Test Data                  | Use Case                                                  |
|------|-----------------------|------------------------------------------------------------------------------------------------------------------------------|----------------------------|-----------------------------------------------------------|
| 1    | Find owner            | The Clinic User opens Find Owners and searches for the owner's last name                                                     | Escobito                   | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details  | The Owner Details view of the owner found in step 1 shows her address and lists the pet "Mulligan" without visits            | -                          | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Verify pet on record  | The pet the caller asks about is listed, so no pet has to be added                                                           | -                          | -                                                         |
| 4    | Book visit            | The Clinic User chooses "Add Visit" next to "Mulligan", keeps the pre-filled date and enters a description                    | limping on front left leg  | [UC-009](../use_cases/UC-009-book-visit-for-pet.md)       |
| 5    | Verify visit recorded | The notification "Your visit has been booked" is shown and the visit appears in Mulligan's visit history on the Owner Details view | -                     | -                                                         |

## Validation

1. **Visit history**: "Mulligan" has exactly one visit, dated today, with the description "limping on front left leg".
2. **Owner unchanged**: Maria Escobito's details (345 Maple St., Madison, 6085557683) and her single pet "Mulligan" (dog, born 1997-02-24) are unchanged.
3. **Nothing else changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey — no owner and no pet was added.

## Postconditions

- One visit for "Mulligan", dated the day the test ran, with description "limping on front left leg", exists.
- Cleanup: delete that visit. The seeded owner "Maria Escobito" and her pet "Mulligan" remain untouched.
