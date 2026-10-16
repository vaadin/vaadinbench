# Test Case: Owner Already on Record

## Overview

**ID:** TC-007  
**Goal:** A clinic user checks whether a caller who wants to register is already a customer, finds the owner and registers nobody — verifying no duplicate owner is created.  
**Priority:** Medium  
**Status:** Approved  
**Process:** [BP-002 Register New Owner](../processes/BP-002-register-new-owner.bpmn) — New client wants to register → UC-004 Find Owners by Last Name → Owner already on record? yes → Owner already on record

## Roles

- Clinic User (checks whether the caller is already on record)

## Preconditions

- The owner "David Schroeder", 2749 Blackhawk Trail, Madison, 6085559435, exists and is the only owner whose last name starts with "Schroeder" (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner already on record? yes" and makes the search land directly on his details.

## Flow

| Step | Name                   | Description                                                                                                                                | Test Data | Use Case                                                  |
|------|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|-----------|-----------------------------------------------------------|
| 1    | Find owner             | The Clinic User opens Find Owners and searches for the caller's last name                                                                  | Schroeder | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Verify owner on record | The search lands on the Owner Details view of David Schroeder, 2749 Blackhawk Trail, Madison, 6085559435, so no owner has to be registered | -         | -                                                         |

## Validation

1. **Owner found, not duplicated**: Searching Find Owners for "Schroeder" still leads straight to the one owner David Schroeder.
2. **Nothing changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey. No owner or pet was added or changed.

## Postconditions

- The journey creates and changes nothing, so there is nothing to clean up.
