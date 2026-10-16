# Test Case: Pet Details Corrected

## Overview

**ID:** TC-005  
**Goal:** A clinic user finds an owner among several with the same last name and corrects the birth date of one of the owner's pets — verifying the corrected pet is shown under the right owner.  
**Priority:** Medium  
**Status:** Approved  
**Process:** [BP-006 Update Pet](../processes/BP-006-update-pet.bpmn) — Owner reports changed pet details → UC-004 Find Owners by Last Name → Owner on record? yes → UC-005 View Owner Details → Pet on record? yes → UC-008 Update Pet → Pet details updated

## Roles

- Clinic User (finds the owner and corrects the pet)

## Preconditions

- Two owners whose last name starts with "Davis" exist — "Betty Davis" and "Harold Davis" (Flyway test data `V2__seed_reference_data.sql`) — so the search shows the Owners List and forces "Owner on record? yes".
- Harold Davis owns the pet "Iggy" (lizard, born 2000-11-30) (Flyway test data `V2__seed_reference_data.sql`) — this forces "Pet on record? yes".

## Flow

| Step | Name                  | Description                                                                                                    | Test Data            | Use Case                                                  |
|------|-----------------------|----------------------------------------------------------------------------------------------------------------|----------------------|-----------------------------------------------------------|
| 1    | Find owner            | The Clinic User opens Find Owners, searches for the last name and selects the matching owner from the list      | Davis, Harold Davis  | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details  | The Owner Details view of the owner selected in step 1 lists the pet "Iggy" with the birth date currently on record | -                | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Update pet            | The Clinic User chooses "Edit Pet" next to "Iggy", replaces the birth date and submits the form; name and type stay as they are | 2000-11-03 | [UC-008](../use_cases/UC-008-update-pet.md)               |
| 4    | Verify pet updated    | The notification "Pet details has been edited" is shown and the Owner Details view lists "Iggy" with the new birth date | -               | -                                                         |

## Validation

1. **Correction visible**: Harold Davis's details list "Iggy" (lizard) with birth date 2000-11-03.
2. **Right owner only**: Betty Davis's details still list only "Basil" (hamster, born 2002-08-06).
3. **Nothing else changed**: Harold Davis's own details (563 Friendly St., Windsor, 6085553198) are unchanged and "Iggy" still has no visits; searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey — no owner and no pet was added.

## Postconditions

- The seeded pet "Iggy" of "Harold Davis" has birth date 2000-11-03.
- Cleanup: restore Iggy's birth date to 2000-11-30 — the seeded record must end as the seed data defines it. Nothing is deleted.
