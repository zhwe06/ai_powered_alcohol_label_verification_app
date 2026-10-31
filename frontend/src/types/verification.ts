export type VerificationStatus =
  | 'PASS'
  | 'FAIL'
  | 'NEEDS_REVIEW'

export interface FieldCheck {
  field: string
  expected: string | null
  detected: string | null
  status: VerificationStatus
  reason: string
}

export interface VerificationResult {
  status: VerificationStatus
  checks: FieldCheck[]
}

export interface ApplicationData {
  brandName: string
  classType: string
  alcoholContent: string
  netContents: string
}