# Test Case: Visit Booked for New Pet

## Overview

**ID:** TC-002  
**Goal:** A clinic user finds an existing owner who brings an animal the clinic has not seen before, adds that pet to the owner and books its first visit — verifying the new pet and its visit appear next to the owner's existing pet.  
**Priority:** High  
**Status:** Approved  
**Process:** [BP-004 Add Pet](../processes/BP-004-add-pet.bpmn), then [BP-005 Book Visit](../processes/BP-005-book-visit.bpmn) — Owner reports a new animal → UC-004 Find Owners by Last Name → Owner on record? yes → UC-005 View Owner Details → UC-007 Add Pet to Owner → Animal on record; the visit is then booked for the new pet: UC-009 Book Visit for Pet → Visit booked

## Roles

- Clinic User (finds the owner, adds the pet and books the visit)

## Preconditions

- The owner "Jeff Black", 1450 Oak Blvd., Monona, 6085555387, exists and is the only owner whose last name starts with "Black" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner on record? yes".
- Jeff Black owns only the pet "Lucky" (bird) and no pet named "Pepper" (Flyway test data `V2__seed_reference_data.sql`) — the pet the caller brings is not on record yet, which is why BP-004 Add Pet runs before BP-005 Book Visit.
- The pet type "hamster" exists (Flyway test data `V2__seed_reference_data.sql`).

## Flow

| Step | Name                  | Description                                                                                                                 | Test Data                 | Use Case                                                  |
|------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------|---------------------------|-----------------------------------------------------------|
| 1    | Find owner            | The Clinic User opens Find Owners and searches for the owner's last name                                                    | Black                     | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details  | The Owner Details view of the owner found in step 1 lists only the pet "Lucky" — the animal the caller brings is not on record | -                      | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Add pet               | The Clinic User chooses "Add New Pet" on that owner's details and submits name, birth date and type                         | Pepper, 2024-02-10, hamster | [UC-007](../use_cases/UC-007-add-pet-to-owner.md)       |
| 4    | Verify pet listed     | The notification "New Pet has been Added" is shown and the Owner Details view lists "Pepper" next to "Lucky"                  | -                         | -                                                         |
| 5    | Book visit            | The Clinic User chooses "Add Visit" next to the pet added in step 3, keeps the pre-filled date and enters a description       | first check-up            | [UC-009](../use_cases/UC-009-book-visit-for-pet.md)       |
| 6    | Verify visit recorded | The notification "Your visit has been booked" is shown and the visit appears in Pepper's visit history on the Owner Details view | -                      | -                                                         |

## Validation

1. **Pet belongs to the owner**: The Owner Details view of Jeff Black lists two pets, "Lucky" (bird) and "Pepper" (hamster, born 2024-02-10).
2. **Visit attributed to the new pet**: "Pepper" has exactly one visit, dated today, with the description "first check-up"; "Lucky" still has no visits.
3. **Nothing else changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey — except that Jeff Black's pets are now "Lucky" and "Pepper". No owner was added and no other owner gained a pet.

## Postconditions

- One pet "Pepper" (birth date 2024-02-10, type "hamster") belonging to "Jeff Black" exists.
- One visit for "Pepper", dated the day the test ran, with description "first check-up", exists.
- Cleanup order: the visit must be deleted before the pet. The seeded owner "Jeff Black" and his pet "Lucky" remain untouched.
