# Test Case: Veterinarian Specialties Found

## Overview

**ID:** TC-006  
**Goal:** A visitor arrives on the welcome page and finds out which veterinarian holds which specialty, without any customer data being involved.  
**Priority:** Low  
**Status:** Approved  
**Process:** [BP-001 View Veterinarians](../processes/BP-001-view-veterinarians.bpmn) — Visitor looks for a veterinarian → UC-001 View Welcome Page → UC-002 View Veterinarians → Specialties known

## Roles

- Visitor (anonymous; opens the application and browses the veterinarians)

## Preconditions

- The six veterinarians James Carter, Helen Leary, Linda Douglas, Rafael Ortega, Henry Stevens and Sharon Jenkins exist with their specialties (Flyway test data `V2__seed_reference_data.sql`).

## Flow

| Step | Name                     | Description                                                                                          | Test Data | Use Case                                                |
|------|--------------------------|------------------------------------------------------------------------------------------------------|-----------|---------------------------------------------------------|
| 1    | Open welcome page        | The Visitor opens the application's start page                                                      | -         | [UC-001](../use_cases/UC-001-view-welcome-page.md)      |
| 2    | Verify navigation offered | The welcome page is shown with navigation links for Home, Find Owners, Veterinarians and Error      | -         | -                                                       |
| 3    | View veterinarians       | The Visitor follows the "Veterinarians" link                                                          | -         | [UC-002](../use_cases/UC-002-view-veterinarians.md)     |
| 4    | Verify specialties shown | The veterinarians grid lists every veterinarian with their specialties                                | -         | -                                                       |

## Validation

1. **All veterinarians listed**: The grid shows six veterinarians.
2. **Specialties readable**: "Linda Douglas" is shown with "dentistry, surgery", "Helen Leary" with "radiology", and "James Carter" with "none".

## Postconditions

- The journey creates and changes nothing; no cleanup is needed.
