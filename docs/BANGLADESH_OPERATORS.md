# Bangladesh Operator Capability Matrix

Research date: **2026-10-24**. This file separates documented capability from
experimental verification. Callora has performed **no live carrier testing**;
every row below reflects documentation research only and is mirrored in the
in-app `OperatorCapabilityRegistry`.

## How to read the statuses

- **Official** — evidence on an operator-controlled domain.
- **Documented** — credible secondary sources and/or the relevant 3GPP/GSM
  standard, without operator confirmation.
- **NOT VERIFIED** — no adequate evidence; Callora still exposes the standard
  GSM workflow but shows an explicit warning.
- **Experimental / CARRIER VERIFIED** — live testing performed by the project.
  None exists yet for any operator.

## Identifiers (MCC 470 — Bangladesh)

| Operator | MCC-MNC | Mobile prefixes | Evidence |
| --- | --- | --- | --- |
| Grameenphone | 47001 | 017, 013 | ITU region-4xx registry (Wikipedia mirror) |
| Robi | 47002 | 018, 016 | Same |
| Banglalink | 47003 | 019, 014 | Same |
| Teletalk | 47004 | 015 | Same |
| Airtel Bangladesh | 47007 | 016 (shared post-merger) | Same; brand retained under Robi Axiata |

## Capability matrix

| Operator | CFU | CFB | CFNRy | CFNRc | Query | Active while powered off | Charging evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Grameenphone | Official | Documented | Documented | **Official** | Documented (`*#`) | Documented (network-side design) | Secondary: activation free, forwarded calls charged |
| Robi | Documented | Documented | Documented | Documented | Documented | Documented | NOT VERIFIED |
| Airtel Bangladesh | Documented (low confidence, 2011) | Documented | Documented | Documented | Documented | Documented | NOT VERIFIED |
| Banglalink | Documented (2015) | Documented | Documented | Documented | Documented | Documented | NOT VERIFIED |
| Teletalk | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED |

All entries share the standard GSM MMI syntax (`**21*`, `**67*`, `**61*`,
`**62*`, `##code#`, `*#code#`); the uncertainty is operator provisioning,
tariffs and eligibility, not the syntax itself.

## Destination / provisioning notes

- Domestic destinations: documented for Grameenphone ("any number" in secondary
  sources); NOT VERIFIED for the other four.
- International destinations: NOT VERIFIED across all operators.
- Service provisioning: secondary evidence says Grameenphone includes call
  forwarding for prepaid and postpaid; all other operators NOT VERIFIED.
- Dual-SIM behaviour depends on the device's ability to select the originating
  subscription; Callora targets a `PhoneAccountHandle` when resolvable and always
  shows a SIM-selection warning.
- LTE/VoLTE/roaming differences are NOT VERIFIED; the Grameenphone official
  evidence is published on its roaming information page.

## Evidence records

1. Grameenphone (official): *Useful Information — GSM Feature Activation/
   Deactivation*, `https://roaming.grameenphone.com/useful-information` — lists
   `**62*number# / ##62#` and `**21*number# / ##21#`.
2. Grameenphone (secondary): GP service case study,
   `https://www.lawyersnjurists.com/article/case-study-grameenphone-service-satisfaction-analysis-customer-point-view`
   — prepaid/postpaid availability, free activation, forwarded-call charging.
3. Robi (secondary): `https://bdsimlink.wordpress.com/2019/09/24/call-settings/`
   — full four-condition divert table with query codes.
4. Airtel Bangladesh (secondary, 2011):
   `http://telecom-infobd.blogspot.com/2011/01/airtel-call-management-services-call_2292.html`.
5. Banglalink (secondary, 2015):
   `https://3g-offer.blogspot.com/2015/08/banglalink-call-divert-forwarding-process-short-codes.html`.
6. GSM standard syntax: `https://en.wikipedia.org/wiki/Call_forwarding`.
7. MCC/MNC registry:
   `https://en.wikipedia.org/wiki/Mobile_Network_Codes_in_ITU_region_4xx_(Asia)`.
8. Cross-operator summary (secondary):
   `https://www.mimsms.com/call-divert-codes-bangladesh`.

## User verification procedure (replaces unverified claims)

For any operator/condition marked NOT VERIFIED:

1. Check current state with the query code (e.g. `*#62#` for CFNRc).
2. Register the rule with a controlled destination you can observe, e.g.
   `**62*01XXXXXXXXX#`.
3. For CFNRc: enable flight mode or power the device off, place a test call from
   a second phone, and confirm the destination rings while the primary is
   unreachable.
4. Power the primary back on, dial `*#62#` again, and record the result inside
   Callora's result dialog so provenance stays honest.
5. Never test emergency-call routing.

## Maintenance rule

Do not promote a NOT VERIFIED row without a dated evidence record (official
source preferred). Update `OperatorCapabilityRegistry.REVIEW_DATE` and this file
together whenever evidence changes.
