# Test Case: Owner Not Found for Visit

## Overview

**ID:** TC-011  
**Goal:** A clinic user searches for a caller who requests a visit, finds no owner with that last name and stops — verifying the search reports the miss and nothing is recorded.  
**Priority:** Low  
**Status:** Approved  
**Process:** [BP-005 Book Visit](../processes/BP-005-book-visit.bpmn) — Owner requests a visit → UC-004 Find Owners by Last Name → Owner on record? no → Owner not on record

## Roles

- Clinic User (searches for the owner)

## Preconditions

- No owner whose last name starts with "Tanaka" exists — the seeded owners use other last names (Flyway test data `V2__seed_reference_data.sql`) — this forces "Owner on record? no".

## Flow

| Step | Name                   | Description                                                                                   | Test Data | Use Case                                                  |
|------|------------------------|-----------------------------------------------------------------------------------------------|-----------|-----------------------------------------------------------|
| 1    | Find owner             | The Clinic User opens Find Owners and searches for the caller's last name                     | Tanaka    | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 2    | Verify owner not found | Find Owners stays open and the last-name field shows "not found"; no Owner Details view opens | -         | -                                                         |

## Validation

1. **Search reports the miss**: After the search, the Find Owners form is still shown with "not found" on the last-name field, and no Owner Details view was opened.
2. **Nothing changed**: Searching Find Owners with an empty last name lists the same owners, with the same name, address, city, telephone and pets, as before the journey. No owner or pet was added or changed.

## Postconditions

- The journey creates and changes nothing, so there is nothing to clean up.
