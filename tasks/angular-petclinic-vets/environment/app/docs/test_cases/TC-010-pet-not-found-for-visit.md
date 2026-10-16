# Test Case: Pet Not Found for Visit

## Overview

**ID:** TC-010  
**Goal:** A clinic user finds the owner who requests a visit but not the animal the visit is for, and books nothing — verifying no visit is recorded.  
**Priority:** Low  
**Status:** Approved  
**Process:** [BP-005 Book Visit](../processes/BP-005-book-visit.bpmn) — Owner requests a visit → UC-004 Find Owners by Last Name → Owner on record? yes → UC-005 View Owner Details → Pet on record? no → Pet not on record

## Roles

- Clinic User (finds the owner and looks for the pet)

## Preconditions

- The owner "Peter McTavish", 2387 S. Fair Way, Madison, 6085552765, exists and is the only owner whose last name starts with "McTavish" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner on record? yes".
- Peter McTavish owns only the pet "George" (snake, born 2000-01-20), which has no visits, and no pet named "Bella" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Pet on record? no".

## Flow

| Step | Name                     | Description                                                                                   | Test Data | Use Case                                                  |
|------|--------------------------|-----------------------------------------------------------------------------------------------|-----------|-----------------------------------------------------------|
| 1    | Find owner               | The Clinic User opens Find Owners and searches for the owner's last name                      | McTavish  | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details     | The Owner Details view of the owner found in step 1 lists his pets                            | -         | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Verify pet not on record | The pet the caller wants a visit for is not listed — only "George" is — so no visit is booked | Bella     | -                                                         |

## Validation

1. **No visit booked**: "George" still has no visits, and Peter McTavish still owns only "George".
2. **Nothing changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey. No owner or pet was added or changed.

## Postconditions

- The journey creates and changes nothing, so there is nothing to clean up.
