# Test Case: Pet Not Found for Correction

## Overview

**ID:** TC-012  
**Goal:** A clinic user finds the owner who reports changed pet details but not the animal they name, and changes nothing — verifying the owner's pets are untouched.  
**Priority:** Low  
**Status:** Approved  
**Process:** [BP-006 Update Pet](../processes/BP-006-update-pet.bpmn) — Owner reports changed pet details → UC-004 Find Owners by Last Name → Owner on record? yes → UC-005 View Owner Details → Pet on record? no → Pet not on record

## Roles

- Clinic User (finds the owner and looks for the pet)

## Preconditions

- The owner "Eduardo Rodriquez", 2693 Commerce St., McFarland, 6085558763, exists and is the only owner whose last name starts with "Rodriquez" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner on record? yes".
- Eduardo Rodriquez owns the pets "Jewel" (dog, born 2000-03-07) and "Rosy" (dog, born 2001-04-17) and no pet named "Bruno" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Pet on record? no".

## Flow

| Step | Name                     | Description                                                                                | Test Data | Use Case                                                  |
|------|--------------------------|--------------------------------------------------------------------------------------------|-----------|-----------------------------------------------------------|
| 1    | Find owner               | The Clinic User opens Find Owners and searches for the owner's last name                   | Rodriquez | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Review owner details     | The Owner Details view of the owner found in step 1 lists his pets                         | -         | [UC-005](../use_cases/UC-005-view-owner-details.md)       |
| 3    | Verify pet not on record | The pet the caller names is not listed — only "Jewel" and "Rosy" are — so no pet is edited | Bruno     | -                                                         |

## Validation

1. **Pets untouched**: Eduardo Rodriquez still owns exactly "Jewel" (born 2000-03-07) and "Rosy" (born 2001-04-17).
2. **Nothing changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey. No owner or pet was added or changed.

## Postconditions

- The journey creates and changes nothing, so there is nothing to clean up.
