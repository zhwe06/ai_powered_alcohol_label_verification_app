import type {
  ApplicationData,
  VerificationResult,
} from '../types/verification'

export async function verifyLabel(
  image: File,
  application: ApplicationData
): Promise<VerificationResult> {
  const formData = new FormData()

  formData.append('image', image)
  formData.append(
    'application',
    JSON.stringify(application)
  )

  const response = await fetch(
    '/api/v1/verifications',
    {
      method: 'POST',
      body: formData,
    }
  )

  if (!response.ok) {
    let message = `Verification failed (HTTP ${response.status})`

    try {
      const error = await response.json()
      message = error.message || error.error || message
    } catch {
      // Response was not JSON.
    }

    throw new Error(message)
  }

  return (await response.json()) as VerificationResult
}
