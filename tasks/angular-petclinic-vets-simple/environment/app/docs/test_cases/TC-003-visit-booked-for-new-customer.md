# Test Case: Visit Booked for New Customer

## Overview

**ID:** TC-003  
**Goal:** A clinic user searches for a caller who turns out not to be a customer yet, registers the owner, adds the pet and books its first visit — verifying owner, pet and visit are linked on the Owner Details view.  
**Priority:** Critical  
**Status:** Approved  
**Process:** [BP-002 Register New Owner](../processes/BP-002-register-new-owner.bpmn), then [BP-004 Add Pet](../processes/BP-004-add-pet.bpmn), then [BP-005 Book Visit](../processes/BP-005-book-visit.bpmn) — New client wants to register → UC-004 Find Owners by Last Name → Owner already on record? no → UC-003 Register New Owner → Owner registered; then UC-007 Add Pet to Owner → Animal on record; then UC-009 Book Visit for Pet → Visit booked

## Roles

- Clinic User (searches for the owner, registers them, adds the pet and books the visit)

## Preconditions

- No owner whose last name starts with "Keller" exists — the seeded owners use other last names (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner already on record? no".
- The pet type "dog" exists (Flyway test data `V2__seed_reference_data.sql`).

## Flow

| Step | Name                  | Description                                                                                                                  | Test Data                                             | Use Case                                                  |
|------|-----------------------|------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|-----------------------------------------------------------|
| 1    | Find owner            | The Clinic User opens Find Owners and searches for the caller's last name                                                    | Keller                                                | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Verify owner unknown  | The Find Owners form is shown again with "not found" on the last-name field                                                  | -                                                     | -                                                         |
| 3    | Register owner        | The Clinic User chooses "Add Owner", fills in the owner form and submits it                                                   | Anna, Keller, 17 Birch Road, Middleton, 6085550123    | [UC-003](../use_cases/UC-003-register-new-owner.md)       |
| 4    | Verify owner created  | The notification "New Owner Created" is shown and the Owner Details view displays the submitted owner with no pets            | -                                                     | -                                                         |
| 5    | Add pet               | The Clinic User chooses "Add New Pet" on the details of the owner registered in step 3 and submits name, birth date and type   | Milo, 2024-08-01, dog                                 | [UC-007](../use_cases/UC-007-add-pet-to-owner.md)         |
| 6    | Verify pet listed     | The notification "New Pet has been Added" is shown and the Owner Details view lists "Milo"                                   | -                                                     | -                                                         |
| 7    | Book visit            | The Clinic User chooses "Add Visit" next to the pet added in step 5, keeps the pre-filled date and enters a description        | puppy vaccination                                     | [UC-009](../use_cases/UC-009-book-visit-for-pet.md)       |
| 8    | Verify visit recorded | The notification "Your visit has been booked" is shown and the visit appears in Milo's visit history on the Owner Details view | -                                                    | -                                                         |

## Validation

1. **Owner is findable**: After the flow, searching Find Owners for "Keller" leads to the details of Anna Keller, 17 Birch Road, Middleton, 6085550123.
2. **Pet belongs to the owner**: Anna Keller's details list exactly one pet, "Milo" (dog, born 2024-08-01).
3. **Visit history**: "Milo" has exactly one visit, dated today, with the description "puppy vaccination".
4. **Nothing else changed**: Searching Find Owners with an empty last name lists exactly one owner more than before the journey — Anna Keller, 17 Birch Road, Middleton, 6085550123, with the pet Milo — and every seeded owner is listed with the same name, address, city, telephone and pets as before. Jean Coleman's seeded visits (2008-09-04 spayed, 2009-06-04 neutered, 2010-03-04 rabies shot, 2011-03-04 rabies shot) are unchanged — the seed data holds no other visits.

## Postconditions

- One owner "Anna Keller", 17 Birch Road, Middleton, telephone 6085550123, exists.
- One pet "Milo" (birth date 2024-08-01, type "dog") belonging to that owner exists.
- One visit for "Milo", dated the day the test ran, with description "puppy vaccination", exists.
- Cleanup order: the visit must be deleted before the pet, and the pet before the owner. The seeded data remains untouched.
